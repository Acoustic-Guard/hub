package com.acousticguard.hub.telemetry.dto;

import com.acousticguard.hub.common.enums.SystemStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Data Transfer Object for system telemetry responses.
 * <p>
 * Contains aggregated system-wide metrics including active node count,
 * average latency, noise levels, and system health status indicators.
 * This DTO is used to serialize telemetry data for REST API responses
 * and WebSocket broadcasts. Null values are excluded from JSON serialization
 * to reduce payload size.
 * </p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TelemetryResponseDto(
        /**
         * Number of currently active sensor nodes.
         */
        Integer activeNodes,
        /**
         * Average network latency across all sensors in milliseconds.
         */
        Integer avgLatencyMs,
        /**
         * Average noise level across all sensors in decibels.
         */
        Integer noiseLevelDb,
        /**
         * System health status based on node count (NORMAL or CRITICAL).
         */
        SystemStatus nodesStatus,
        /**
         * System health status based on latency (NORMAL or WARNING).
         */
        SystemStatus latencyStatus,
        /**
         * System health status based on noise levels (NORMAL or WARNING).
         */
        SystemStatus noiseStatus,

        /**
         * Number of offline sensor nodes.
         */
        Integer offlineNodes,
        /**
         * Number of sensors with warning status.
         */
        Integer warningNodes,
        /**
         * Overall system uptime percentage.
         */
        Float uptimePercent,
        /**
         * Timestamp when telemetry was last updated.
         */
        Instant lastUpdatedAt,
        /**
         * Number of events processed per minute.
         */
        Integer eventsPerMinute
) {
}