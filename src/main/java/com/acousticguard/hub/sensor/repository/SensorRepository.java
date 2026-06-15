package com.acousticguard.hub.sensor.repository;

import com.acousticguard.hub.common.enums.SensorStatus;
import com.acousticguard.hub.sensor.model.Sensor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Sensor entity persistence.
 * <p>
 * Provides data access operations for sensor entities, including queries for
 * heartbeat monitoring and noise data retrieval. This repository extends JpaRepository
 * for standard CRUD operations. Custom query methods support sensor health monitoring
 * and filtering by status and noise data availability.
 * </p>
 */
public interface SensorRepository extends JpaRepository<Sensor, String> {

    /**
     * Finds a sensor by its unique identifier.
     * <p>
     * This method retrieves a sensor by its string ID. Returns an empty Optional
     * if the sensor does not exist.
     * </p>
     *
     * @param sensorId the unique identifier of the sensor
     * @return an Optional containing the sensor if found, or empty if not found
     */
    Optional<Sensor> findById(String sensorId);

    /**
     * Finds sensors that have not sent a heartbeat since the specified timestamp.
     * <p>
     * This method is used by the health monitoring service to identify sensors
     * that may have gone offline. Sensors with last heartbeat before the threshold
     * are candidates for status transition to OFFLINE.
     * </p>
     *
     * @param before the timestamp threshold (sensors with last heartbeat before this time)
     * @return a list of sensors with last heartbeat before the threshold
     */
    List<Sensor> findByLastHeartbeatBefore(Instant before);

    /**
     * Finds sensors with the specified status and non-null current average decibel level.
     * <p>
     * This method is used to retrieve sensors that are actively transmitting noise data.
     * Only sensors with the specified status and a current average decibel level
     * (indicating recent noise data) are returned.
     * </p>
     *
     * @param status the sensor status to filter by (e.g., ONLINE, OFFLINE)
     * @return a list of sensors with the specified status and non-null current average decibel level
     */
    List<Sensor> findByStatusAndCurrentAvgDbIsNotNull(SensorStatus status);
}
