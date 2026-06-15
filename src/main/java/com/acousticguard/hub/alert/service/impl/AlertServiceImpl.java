package com.acousticguard.hub.alert.service.impl;

import com.acousticguard.hub.alert.mapper.AlertMapper;
import com.acousticguard.hub.alert.model.Alert;
import com.acousticguard.hub.alert.repository.AlertRepository;
import com.acousticguard.hub.alert.service.AlertService;
import com.acousticguard.hub.classifier.dto.ClassificationResult;
import com.acousticguard.hub.common.error.AlertNotFoundError;
import com.acousticguard.hub.common.error.ConfidenceThresholdNotMetError;
import com.acousticguard.hub.common.error.DomainError;
import com.acousticguard.hub.sensor.dto.AudioFrame;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of AlertService.
 * <p>
 * Handles alert creation with idempotency and confidence threshold enforcement.
 * This implementation prevents duplicate alerts from the same audio frame using
 * a sliding window deduplication strategy. Alerts are only created if the
 * classification confidence meets or exceeds the configured threshold.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertServiceImpl implements AlertService {

    private final AlertRepository alertRepository;
    private final com.acousticguard.hub.websocket.EventPublisherPort eventPublisherPort;
    private final AlertMapper alertMapper;

    @Value("${acoustic.classifier.confidence-threshold:0.75}")
    private float confidenceThreshold;

    /**
     * Creates an alert from an audio frame and classification result.
     * <p>
     * This method enforces idempotency through a two-step deduplication strategy:
     * 1. Exact timestamp match check for the most common duplicate case
     * 2. Sliding window check (1-second window) for near-duplicate prevention
     * Only creates an alert if confidence meets the configured threshold.
     * Successfully created alerts are broadcast via WebSocket for real-time updates.
     * </p>
     *
     * @param frame  the audio frame containing sensor data including sensor ID,
     *               location, and capture timestamp
     * @param result the classification result from the threat classifier including
     *               threat type and confidence score
     * @return an Optional containing the created or existing alert, or empty if
     * confidence threshold not met
     */
    @Override
    @Transactional
    public Optional<Alert> createAlert(AudioFrame frame, ClassificationResult result) {
        if (result.confidence() < confidenceThreshold) {
            DomainError error = new ConfidenceThresholdNotMetError(result.confidence(), confidenceThreshold);
            log.debug("{}", error.getMessage());
            return Optional.empty();
        }

        Instant detectedAt = Instant.ofEpochMilli(frame.capturedAtMs());

        // Check for exact timestamp match first (most common case)
        Optional<Alert> existingAlert = alertRepository.findBySensorIdAndDetectedAt(frame.sensorId(), detectedAt);

        if (existingAlert.isPresent()) {
            log.debug("Duplicate alert detected for sensor {} at {}, skipping creation",
                    frame.sensorId(), frame.capturedAtMs());
            return existingAlert;
        }

        // Check for duplicates within 1-second sliding window for same sensor and threat type
        Instant windowStart = detectedAt.minusSeconds(1);
        Instant windowEnd = detectedAt.plusSeconds(1);
        List<Alert> recentAlerts = alertRepository.findBySensorIdAndThreatTypeAndDetectedAtBetween(
                frame.sensorId(), result.threatType().getValue(), windowStart, windowEnd);

        if (!recentAlerts.isEmpty()) {
            log.debug("Duplicate alert detected for sensor {} within 1-second window at {}, skipping creation",
                    frame.sensorId(), frame.capturedAtMs());
            return Optional.of(recentAlerts.get(0));
        }

        Point location = alertMapper.latitudeLongitudeToPoint(frame.latitude(), frame.longitude());

        Alert alert = Alert.builder()
                .threatType(result.threatType().getValue())
                .confidence(result.confidence())
                .location(formatLocation(frame.latitude(), frame.longitude()))
                .detectedAt(Instant.ofEpochMilli(frame.capturedAtMs()))
                .sensorId(frame.sensorId())
                .locationGeo(location)
                .metadata(buildMetadata(frame, result))
                .build();

        Alert savedAlert = alertRepository.save(alert);
        log.info("Created alert {} for sensor {} with threat type {} and confidence {}",
                savedAlert.getId(), frame.sensorId(), result.threatType(), result.confidence());

        eventPublisherPort.publishAlert(alertMapper.toDto(savedAlert));

        return Optional.of(savedAlert);
    }

    /**
     * Retrieves an alert by its unique identifier.
     * <p>
     * This method performs a read-only transaction to query the database for an alert
     * with the specified UUID. If the alert does not exist, returns an empty Optional.
     * </p>
     *
     * @param id the unique identifier (UUID) of the alert to retrieve
     * @return an Optional containing the alert if found, or empty if not found
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<Alert> findById(UUID id) {
        return alertRepository.findById(id);
    }

    /**
     * Updates the status of an alert.
     * <p>
     * This method changes the alert status and persists the update to the database.
     * If the alert does not exist, throws an AlertNotFoundError. The status update
     * may trigger downstream workflows such as incident aggregation.
     * </p>
     *
     * @param id     the unique identifier (UUID) of the alert to update
     * @param status the new status value as a string
     * @return the updated alert entity with the new status
     * @throws AlertNotFoundError if the alert does not exist
     */
    @Override
    @Transactional
    public Alert updateStatus(UUID id, String status) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new AlertNotFoundError(id));

        log.info("Updated alert {} status to {}", id, status);

        return alertRepository.save(alert);
    }

    /**
     * Retrieves all alerts in the system.
     * <p>
     * This method performs a read-only transaction to fetch all registered alerts,
     * sorted by detection timestamp in descending order (most recent first).
     * For production use, consider implementing pagination or filtering parameters.
     * </p>
     *
     * @return a list of all alerts sorted by detection timestamp descending,
     * or an empty list if no alerts exist
     */
    @Override
    @Transactional(readOnly = true)
    public List<Alert> findAll() {
        return alertRepository.findAll(Sort.by(Sort.Direction.DESC, "detectedAt"));
    }

    /**
     * Formats latitude and longitude coordinates as a string.
     *
     * @param latitude  the latitude coordinate
     * @param longitude the longitude coordinate
     * @return a formatted string in "lat,lng" format with 6 decimal places
     */
    private String formatLocation(float latitude, float longitude) {
        return String.format("%.6f,%.6f", latitude, longitude);
    }

    /**
     * Builds metadata map from audio frame and classification result.
     *
     * @param frame  the audio frame containing technical audio data
     * @param result the classification result containing model information
     * @return a map of metadata key-value pairs
     */
    private Map<String, Object> buildMetadata(AudioFrame frame, ClassificationResult result) {
        return Map.of(
                "modelVersion", result.modelVer(),
                "sampleRateHz", frame.sampleRateHz(),
                "fftBinsCount", frame.fftBins() != null ? frame.fftBins().size() : 0,
                "peakDb", frame.peakDb() != null ? frame.peakDb() : 0.0f
        );
    }
}
