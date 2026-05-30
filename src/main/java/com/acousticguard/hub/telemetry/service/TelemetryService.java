package com.acousticguard.hub.telemetry.service;

import com.acousticguard.hub.telemetry.dto.TelemetryResponseDto;

/**
 * Service interface for telemetry management.
 * Defines business operations for collecting and reporting system telemetry.
 */
public interface TelemetryService {

    /**
     * Updates the noise level for a specific sensor node.
     * 
     * @param sensorId the sensor identifier
     * @param avgDb the average decibel level
     */
    void updateNodeNoiseLevel(String sensorId, Float avgDb);

    /**
     * Retrieves current system telemetry including active nodes, noise levels, and system status.
     * 
     * @return the current system telemetry
     */
    TelemetryResponseDto getSystemTelemetry();
}