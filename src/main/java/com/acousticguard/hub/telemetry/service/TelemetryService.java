package com.acousticguard.hub.telemetry.service;

import com.acousticguard.hub.sensor.dto.AudioFrame;
import com.acousticguard.hub.telemetry.dto.TelemetryResponseDto;

/**
 * Service interface for telemetry management.
 * Defines business operations for collecting and reporting system telemetry.
 */
public interface TelemetryService {

    /**
     * Updates telemetry data for a specific sensor node from an audio frame.
     * Stores noise level, latency, latitude, and longitude via NodeState.
     * 
     * @param frame the audio frame containing telemetry data
     */
    void updateNodeTelemetry(AudioFrame frame);

    /**
     * Retrieves current system telemetry including active nodes, noise levels, and system status.
     * 
     * @return the current system telemetry
     */
    TelemetryResponseDto getSystemTelemetry();
}