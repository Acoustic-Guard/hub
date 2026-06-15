package com.acousticguard.hub.alert.dto;

import com.acousticguard.hub.common.enums.ThreatType;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Data Transfer Object for alert responses.
 * <p>
 * Contains alert information returned to API clients, including threat type,
 * confidence score, location, detection timestamp, and metadata. This DTO is
 * used to serialize alert data for REST API responses and WebSocket broadcasts.
 * Null values are excluded from JSON serialization to reduce payload size.
 * </p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AlertResponseDto(
        /**
         * Unique identifier of the alert.
         */
        UUID id,
        /**
         * The type of threat detected (e.g., GUNSHOT, SCREAM, GLASS_BREAK).
         */
        ThreatType threatType,
        /**
         * The confidence score of the threat classification (0.0 to 1.0).
         */
        Float confidence,
        /**
         * Human-readable location description.
         */
        String location,
        /**
         * The timestamp when the threat was detected.
         */
        Instant detectedAt,

        /**
         * The unique identifier of the sensor that detected the threat.
         */
        String sensorId,
        /**
         * GPS latitude coordinate of the alert location.
         */
        Float latitude,
        /**
         * GPS longitude coordinate of the alert location.
         */
        Float longitude,
        /**
         * Additional metadata about the alert and classification.
         */
        Map<String, Object> metadata
) {
}