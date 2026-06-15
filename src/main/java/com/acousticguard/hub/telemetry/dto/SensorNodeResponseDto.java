package com.acousticguard.hub.telemetry.dto;

import com.acousticguard.hub.common.enums.SensorStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

/**
 * Data Transfer Object for sensor node responses.
 * <p>
 * Contains sensor information returned to API clients, including status,
 * latency, location, and metadata. This DTO is used to serialize sensor data
 * for REST API responses and WebSocket broadcasts. Null values are excluded
 * from JSON serialization to reduce payload size.
 * </p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SensorNodeResponseDto(
        /**
         * Unique identifier of the sensor.
         */
        String id,
        /**
         * Human-readable location description.
         */
        String location,
        /**
         * Current operational status of the sensor (ONLINE or OFFLINE).
         */
        SensorStatus status,
        /**
         * Network latency in milliseconds.
         */
        Integer latencyMs,
        /**
         * Uptime percentage of the sensor.
         */
        Float uptimePercent,
        /**
         * Timestamp of the last heartbeat received.
         */
        Instant lastHeartbeat,


        /**
         * GPS latitude coordinate of the sensor.
         */
        Float latitude,
        /**
         * GPS longitude coordinate of the sensor.
         */
        Float longitude,
        /**
         * Firmware version of the sensor.
         */
        String firmwareVersion,
        /**
         * Additional metadata about the sensor.
         */
        Map<String, Object> metadata
) {
}