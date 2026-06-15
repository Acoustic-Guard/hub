package com.acousticguard.hub.sensor.service;

import com.acousticguard.hub.sensor.model.Sensor;

import java.util.List;

/**
 * Service interface for sensor health monitoring.
 * <p>
 * Handles heartbeat tracking and scheduled offline sensor checking. This service
 * is responsible for monitoring sensor connectivity, detecting offline sensors,
 * and managing status transitions between ONLINE and OFFLINE states. It operates
 * independently of the sensor registry to maintain separation of concerns.
 * </p>
 */
public interface SensorHealthMonitorService {

    /**
     * Updates the heartbeat timestamp for a sensor.
     * <p>
     * This method is called when a heartbeat is received from a sensor, indicating
     * that the sensor is actively transmitting data. The heartbeat is tracked
     * in-memory via the TelemetryService to avoid unnecessary database writes.
     * </p>
     *
     * @param sensorId the unique identifier of the sensor that sent the heartbeat
     */
    void updateHeartbeat(String sensorId);

    /**
     * Checks all sensors and marks those without recent heartbeats as offline.
     * <p>
     * This method is called periodically via scheduled execution (every 5 seconds)
     * to scan all registered sensors and compare their last heartbeat timestamp
     * against the configured timeout threshold. Sensors that have not transmitted
     * within the timeout period are marked as OFFLINE in the database, and
     * status change events are published via WebSocket. Sensors that recover
     * (transmit after being offline) are marked as ONLINE.
     * </p>
     *
     * @return a list of sensors that were newly marked as offline during this check,
     * or an empty list if no sensors transitioned to offline
     */
    List<Sensor> checkOfflineSensors();
}
