package com.acousticguard.hub.sensor.service;

import com.acousticguard.hub.sensor.model.Sensor;

import java.util.List;

/**
 * Service interface for sensor health monitoring.
 * Handles heartbeat tracking and scheduled offline sensor checking.
 */
public interface SensorHealthMonitorService {

    /**
     * Updates the heartbeat timestamp for a sensor.
     * Marks the sensor as online when a heartbeat is received.
     *
     * @param sensorId the sensor identifier
     */
    void updateHeartbeat(String sensorId);

    /**
     * Checks all sensors and marks those without recent heartbeats as offline.
     * This method is called periodically via scheduled execution.
     *
     * @return list of sensors that were marked offline
     */
    List<Sensor> checkOfflineSensors();
}
