package com.acousticguard.hub.telemetry.service;

import com.acousticguard.hub.common.enums.SensorStatus;
import com.acousticguard.hub.telemetry.dto.TelemetryEvent;
import com.acousticguard.hub.telemetry.dto.TelemetryResponseDto;

/**
 * Service interface for telemetry management.
 * Defines business operations for collecting and reporting system telemetry.
 */
public interface TelemetryService {

    /**
     * Updates telemetry data for a specific sensor node from a telemetry event.
     * Stores noise level, latency, latitude, and longitude via NodeState.
     *
     * @param event the telemetry event containing noise map data
     */
    void updateNodeTelemetry(TelemetryEvent event);

    /**
     * Retrieves current system telemetry including active nodes, noise levels, and system status.
     *
     * @return the current system telemetry
     */
    TelemetryResponseDto getSystemTelemetry();

    /**
     * Gets the status of a sensor based on in-memory heartbeat data.
     *
     * @param sensorId the sensor identifier
     * @return the sensor status
     */
    SensorStatus getSensorStatus(String sensorId);

    /**
     * Gets the current latency for a specific sensor.
     *
     * @param sensorId the sensor identifier
     * @return latency in milliseconds, or null if sensor is offline or not found
     */
    Long getSensorLatency(String sensorId);
}