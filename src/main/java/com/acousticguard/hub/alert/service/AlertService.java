package com.acousticguard.hub.alert.service;

import com.acousticguard.hub.alert.model.Alert;
import com.acousticguard.hub.classifier.dto.ClassificationResult;
import com.acousticguard.hub.sensor.dto.AudioFrame;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service interface for alert management.
 * <p>
 * Defines business operations for creating and managing threat alerts. Alerts are
 * generated when the threat classification service detects potential threats in
 * audio frames with confidence above the configured threshold. This service
 * enforces idempotency to prevent duplicate alerts from the same audio frame.
 * </p>
 */
public interface AlertService {

    /**
     * Creates an alert from an audio frame and classification result.
     * <p>
     * This method enforces idempotency by checking for existing alerts with the
     * same sensor ID and capture timestamp. Duplicate frames will not create duplicate
     * alerts. An alert is only created if the classification confidence meets or
     * exceeds the configured threshold (default 0.10). This prevents false positives
     * from low-confidence classifications.
     * </p>
     *
     * @param frame  the audio frame containing sensor data including sensor ID,
     *               location, and capture timestamp
     * @param result the classification result from the threat classifier including
     *               threat type and confidence score
     * @return an Optional containing the created alert if successful, or empty if
     * confidence threshold not met or duplicate detected
     */
    Optional<Alert> createAlert(AudioFrame frame, ClassificationResult result);

    /**
     * Retrieves an alert by its unique identifier.
     * <p>
     * This method queries the database for an alert with the specified UUID.
     * If the alert does not exist, returns an empty Optional.
     * </p>
     *
     * @param id the unique identifier (UUID) of the alert to retrieve
     * @return an Optional containing the alert if found, or empty if not found
     */
    Optional<Alert> findById(UUID id);

    /**
     * Updates the status of an alert.
     * <p>
     * This method changes the alert status (e.g., from PENDING to ACKNOWLEDGED,
     * RESOLVED, etc.). The status update is persisted to the database and may
     * trigger downstream workflows such as incident aggregation or notification
     * dispatch. Invalid alert IDs will result in an IllegalArgumentException.
     * </p>
     *
     * @param id     the unique identifier (UUID) of the alert to update
     * @param status the new status value as a string (e.g., "RESOLVED", "ACKNOWLEDGED")
     * @return the updated alert entity with the new status
     * @throws IllegalArgumentException if the alert does not exist or status is invalid
     */
    Alert updateStatus(UUID id, String status);

    /**
     * Retrieves all alerts in the system.
     * <p>
     * This method returns a complete list of all alerts regardless of their status
     * or age. For production use, consider implementing pagination or filtering
     * parameters to manage large datasets efficiently.
     * </p>
     *
     * @return a list of all alerts in the system, or an empty list if no alerts exist
     */
    List<Alert> findAll();
}
