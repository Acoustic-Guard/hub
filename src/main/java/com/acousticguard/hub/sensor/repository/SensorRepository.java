package com.acousticguard.hub.sensor.repository;

import com.acousticguard.hub.sensor.model.Sensor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Sensor entities.
 * This is a Spring Data JPA repository for sensor data access.
 */
public interface SensorRepository extends JpaRepository<Sensor, String> {

    /**
     * Finds a sensor by its identifier.
     *
     * @param sensorId the sensor identifier
     * @return an Optional containing the sensor if found
     */
    Optional<Sensor> findById(String sensorId);

    /**
     * Finds sensors that have not sent a heartbeat since the specified timestamp.
     *
     * @param before the timestamp threshold
     * @return list of sensors with last heartbeat before the threshold
     */
    List<Sensor> findByLastHeartbeatBefore(Instant before);
}
