package com.acousticguard.hub.alert.service;

import com.acousticguard.hub.alert.model.Alert;
import com.acousticguard.hub.classifier.dto.ClassificationResult;
import com.acousticguard.hub.sensor.dto.AudioFrame;

import java.util.Optional;
import java.util.UUID;

/**
 * Service interface for alert management.
 * Defines business operations for creating and managing threat alerts.
 */
public interface AlertService {

    /**
     * Creates an alert from an audio frame and classification result.
     * Enforces idempotency: duplicate frames (same sensorId + capturedAtMs) will not create duplicate alerts.
     * Only creates an alert if confidence meets the threshold (>= 0.75).
     *
     * @param frame  the audio frame containing sensor data
     * @param result the classification result from the classifier
     * @return the created alert, or empty if confidence threshold not met or duplicate detected
     */
    Optional<Alert> createAlert(AudioFrame frame, ClassificationResult result);

    /**
     * Retrieves an alert by its identifier.
     *
     * @param id the alert identifier
     * @return the alert if found
     */
    Optional<Alert> findById(UUID id);

    /**
     * Updates the status of an alert.
     *
     * @param id     the alert identifier
     * @param status the new status
     * @return the updated alert
     */
    Alert updateStatus(UUID id, String status);
}
