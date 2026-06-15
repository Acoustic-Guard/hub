package com.acousticguard.hub.alert.repository;

import com.acousticguard.hub.alert.model.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for Alert entity persistence.
 * <p>
 * Provides data access operations for alerts, including custom queries for
 * idempotency checks and analytics. This repository extends JpaRepository for
 * standard CRUD operations and JpaSpecificationExecutor for dynamic query
 * building. Custom query methods support alert deduplication and time-based
 * analytics.
 * </p>
 */
@Repository
public interface AlertRepository extends JpaRepository<Alert, UUID>, JpaSpecificationExecutor<Alert> {

    /**
     * Finds an alert by sensor ID and detection timestamp for idempotency.
     * <p>
     * This method is used to prevent duplicate alerts from the same audio frame.
     * It performs an indexed query on the sensor ID and detection timestamp to
     * avoid full table scans. This is the primary deduplication check for the
     * most common duplicate case.
     * </p>
     *
     * @param sensorId   the unique identifier of the sensor
     * @param detectedAt the exact timestamp when the alert was detected
     * @return an Optional containing the alert if found, or empty if not found
     */
    Optional<Alert> findBySensorIdAndDetectedAt(String sensorId, Instant detectedAt);

    /**
     * Finds alerts within a 1-second time window for a specific sensor and threat type.
     * <p>
     * This method is used for idempotency to prevent duplicate alerts during
     * network retries or near-duplicate audio frames. It performs a sliding window
     * check to catch alerts that may have slightly different timestamps but represent
     * the same threat event.
     * </p>
     *
     * @param sensorId   the unique identifier of the sensor
     * @param threatType the type of threat (e.g., "GUNSHOT", "SCREAM")
     * @param start      the start of the time window (detectedAt - 1 second)
     * @param end        the end of the time window (detectedAt + 1 second)
     * @return a list of alerts within the specified time window, or empty if none found
     */
    List<Alert> findBySensorIdAndThreatTypeAndDetectedAtBetween(
            String sensorId, String threatType, Instant start, Instant end);

    /**
     * Counts alerts detected after a specified timestamp.
     * <p>
     * This method is used for analytics to calculate alert rates over time.
     * </p>
     *
     * @param threshold the timestamp threshold (alerts detected after this time)
     * @return the count of alerts detected after the threshold
     */
    long countByDetectedAtAfter(Instant threshold);

    /**
     * Counts alerts detected within a time range.
     * <p>
     * This method is used for analytics to calculate alert rates for specific
     * time periods (e.g., hourly, daily, weekly).
     * </p>
     *
     * @param start the start of the time range
     * @param end   the end of the time range
     * @return the count of alerts detected within the time range
     */
    long countByDetectedAtBetween(Instant start, Instant end);
}