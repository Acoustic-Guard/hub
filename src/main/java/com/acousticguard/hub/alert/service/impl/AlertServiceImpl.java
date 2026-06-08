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
 * Handles alert creation with idempotency and confidence threshold enforcement.
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

    @Override
    @Transactional
    public Optional<Alert> createAlert(AudioFrame frame, ClassificationResult result) {
        if (result.confidence() < confidenceThreshold) {
            DomainError error = new ConfidenceThresholdNotMetError(result.confidence(), confidenceThreshold);
            log.debug("{}", error.getMessage());
            return Optional.empty();
        }

        Instant detectedAt = Instant.ofEpochMilli(frame.capturedAtMs());
        Optional<Alert> existingAlert = alertRepository.findBySensorIdAndDetectedAt(frame.sensorId(), detectedAt);

        if (existingAlert.isPresent()) {
            log.debug("Duplicate alert detected for sensor {} at {}, skipping creation",
                    frame.sensorId(), frame.capturedAtMs());
            return existingAlert;
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

    @Override
    @Transactional(readOnly = true)
    public Optional<Alert> findById(UUID id) {
        return alertRepository.findById(id);
    }

    @Override
    @Transactional
    public Alert updateStatus(UUID id, String status) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new AlertNotFoundError(id));

        log.info("Updated alert {} status to {}", id, status);

        return alertRepository.save(alert);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Alert> findAll() {
        return alertRepository.findAll(Sort.by(Sort.Direction.DESC, "detectedAt"));
    }

    private String formatLocation(float latitude, float longitude) {
        return String.format("%.6f,%.6f", latitude, longitude);
    }

    private Map<String, Object> buildMetadata(AudioFrame frame, ClassificationResult result) {
        return Map.of(
                "modelVersion", result.modelVer(),
                "sampleRateHz", frame.sampleRateHz(),
                "fftBinsCount", frame.fftBins() != null ? frame.fftBins().size() : 0,
                "peakDb", frame.peakDb() != null ? frame.peakDb() : 0.0f,
                "avgDb", frame.avgDb() != null ? frame.avgDb() : 0.0f
        );
    }
}
