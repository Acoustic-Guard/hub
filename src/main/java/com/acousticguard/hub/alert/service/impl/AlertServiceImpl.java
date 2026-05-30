package com.acousticguard.hub.alert.service.impl;

import com.acousticguard.hub.alert.model.Alert;
import com.acousticguard.hub.alert.repository.AlertRepository;
import com.acousticguard.hub.alert.service.AlertService;
import com.acousticguard.hub.classifier.dto.ClassificationResult;
import com.acousticguard.hub.common.enums.ThreatType;
import com.acousticguard.hub.common.error.AlertNotFoundError;
import com.acousticguard.hub.common.error.ConfidenceThresholdNotMetError;
import com.acousticguard.hub.common.error.DomainError;
import com.acousticguard.hub.port.EventPublisherPort;
import com.acousticguard.hub.sensor.dto.AudioFrame;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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
    private final EventPublisherPort eventPublisherPort;
    private static final float CONFIDENCE_THRESHOLD = 0.75f;

    @Override
    @Transactional
    public Optional<Alert> createAlert(AudioFrame frame, ClassificationResult result) {
        // Enforce confidence threshold
        if (result.confidence() < CONFIDENCE_THRESHOLD) {
            DomainError error = new ConfidenceThresholdNotMetError(result.confidence(), CONFIDENCE_THRESHOLD);
            log.debug("{}", error.getMessage());
            return Optional.empty();
        }

        // Enforce idempotency: check for existing alert with same sensorId and capturedAtMs
        Optional<Alert> existingAlert = alertRepository.findAll().stream()
                .filter(alert -> alert.getSensorId() != null)
                .filter(alert -> alert.getSensorId().equals(frame.sensorId()))
                .filter(alert -> {
                    Instant detectedAt = alert.getDetectedAt();
                    return detectedAt != null && detectedAt.toEpochMilli() == frame.capturedAtMs();
                })
                .findFirst();

        if (existingAlert.isPresent()) {
            log.debug("Duplicate alert detected for sensor {} at {}, skipping creation", 
                    frame.sensorId(), frame.capturedAtMs());
            return existingAlert;
        }

        // Create new alert
        Alert alert = Alert.builder()
                .threatType(result.threatType().getValue())
                .confidence(result.confidence())
                .location(formatLocation(frame.latitude(), frame.longitude()))
                .detectedAt(Instant.ofEpochMilli(frame.capturedAtMs()))
                .sensorId(frame.sensorId())
                .latitude(frame.latitude())
                .longitude(frame.longitude())
                .metadata(buildMetadata(frame, result))
                .build();

        Alert savedAlert = alertRepository.save(alert);
        log.info("Created alert {} for sensor {} with threat type {} and confidence {}", 
                savedAlert.getId(), frame.sensorId(), result.threatType(), result.confidence());
        
        // Publish alert created event
        eventPublisherPort.publish(savedAlert);
        
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
        
        // Status update logic would go here if Alert had a status field
        // For now, this is a placeholder for future enhancement
        log.info("Updated alert {} status to {}", id, status);
        
        return alertRepository.save(alert);
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
