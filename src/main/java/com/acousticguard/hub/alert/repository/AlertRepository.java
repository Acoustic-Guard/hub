package com.acousticguard.hub.alert.repository;

import com.acousticguard.hub.alert.model.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AlertRepository extends JpaRepository<Alert, UUID>, JpaSpecificationExecutor<Alert> {

    /**
     * Finds an alert by sensor ID and detection timestamp for idempotency.
     * Uses indexed query to avoid full table scans.
     *
     * @param sensorId   the sensor identifier
     * @param detectedAt the detection timestamp
     * @return the alert if found
     */
    Optional<Alert> findBySensorIdAndDetectedAt(String sensorId, Instant detectedAt);

    /**
     * Finds alerts within a 1-second time window for a specific sensor and threat type.
     * Used for idempotency to prevent duplicate alerts during network retries.
     *
     * @param sensorId   the sensor identifier
     * @param threatType the threat type
     * @param start      the start of the time window
     * @param end        the end of the time window
     * @return list of alerts within the time window
     */
    List<Alert> findBySensorIdAndThreatTypeAndDetectedAtBetween(
            String sensorId, String threatType, Instant start, Instant end);

    long countByDetectedAtAfter(Instant threshold);

    long countByDetectedAtBetween(Instant start, Instant end);
}