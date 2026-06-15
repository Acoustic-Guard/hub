package com.acousticguard.hub.telemetry.service;

import com.acousticguard.hub.common.enums.SensorStatus;
import com.acousticguard.hub.telemetry.dto.TelemetryEvent;
import com.acousticguard.hub.telemetry.dto.TelemetryResponseDto;

/**
 * Service interface for telemetry management.
 * <p>
 * Defines business operations for collecting and reporting system telemetry.
 * This service maintains an in-memory cache of sensor node states to enable
 * real-time telemetry queries without database bottlenecks. It processes
 * telemetry events from edge sensors, calculates network latency, and
 * aggregates system-wide metrics for dashboard visualization.
 * </p>
 */
public interface TelemetryService {

    /**
     * Updates telemetry data for a specific sensor node from a telemetry event.
     * <p>
     * This method processes incoming telemetry events, calculates network latency
     * (time difference between capture and processing), converts dBFS to dB SPL
     * for frontend display, and updates the in-memory node state cache. The method
     * also delegates database persistence to a throttled adapter to prevent
     * write amplification during high-frequency telemetry processing.
     * </p>
     *
     * @param event the telemetry event containing noise map data, sensor location,
     *              and capture timestamp
     */
    void updateNodeTelemetry(TelemetryEvent event);

    /**
     * Retrieves current system telemetry including active nodes, noise levels, and system status.
     * <p>
     * This method aggregates real-time metrics from the in-memory node state cache,
     * calculating average noise levels, latency, and system health status. The
     * computed metrics are returned in a DTO for frontend dashboard consumption.
     * This method is optimized for performance by avoiding database queries.
     * </p>
     *
     * @return a TelemetryResponseDto containing aggregated system metrics including
     * active node count, average latency, noise level, and system status indicators
     */
    TelemetryResponseDto getSystemTelemetry();

    /**
     * Gets the status of a sensor based on in-memory heartbeat data.
     * <p>
     * This method queries the in-memory node state cache to determine if a sensor
     * is ONLINE or OFFLINE. A sensor is considered ONLINE if it has transmitted
     * telemetry within the configured heartbeat timeout period (default 60 seconds).
     * This method is used by the health monitoring service to detect offline sensors.
     * </p>
     *
     * @param sensorId the unique identifier of the sensor to query
     * @return the sensor status (ONLINE if recent heartbeat detected, OFFLINE otherwise)
     */
    SensorStatus getSensorStatus(String sensorId);

    /**
     * Gets the current network latency for a specific sensor.
     * <p>
     * This method retrieves the network latency for a sensor from the in-memory
     * node state cache. Latency is calculated as the time difference between when
     * the sensor captured the data and when the hub processed it. Returns null if
     * the sensor is offline or has not transmitted telemetry recently.
     * </p>
     *
     * @param sensorId the unique identifier of the sensor to query
     * @return the network latency in milliseconds, or null if the sensor is offline
     * or not found in the telemetry cache
     */
    Long getSensorLatency(String sensorId);
}