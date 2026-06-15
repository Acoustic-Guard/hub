package com.acousticguard.hub.incident.dto;

import com.acousticguard.hub.common.enums.IncidentStatus;
import com.acousticguard.hub.common.enums.ThreatType;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Data Transfer Object for incident responses.
 * <p>
 * Contains incident information returned to API clients, including location,
 * threat type, intensity, status, and metadata. This DTO is used to serialize
 * incident data for REST API responses and WebSocket broadcasts. Null values
 * are excluded from JSON serialization to reduce payload size.
 * </p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record IncidentResponseDto(
        /**
         * Unique identifier of the incident.
         */
        UUID id,
        /**
         * GPS latitude coordinate of the incident location.
         */
        Float latitude,
        /**
         * GPS longitude coordinate of the incident location.
         */
        Float longitude,
        /**
         * The type of threat associated with this incident.
         */
        ThreatType type,
        /**
         * The intensity of the incident (maximum confidence of aggregated alerts).
         */
        Float intensity,

        /**
         * The current status of the incident (e.g., DETECTED, INVESTIGATING, CONFIRMED, RESOLVED).
         */
        IncidentStatus status,
        /**
         * The unique identifier of the sensor associated with this incident.
         */
        String sensorId,
        /**
         * The timestamp when the incident was first created.
         */
        Instant createdAt,
        /**
         * The timestamp when the incident was last updated.
         */
        Instant updatedAt,
        /**
         * Additional metadata about the incident (alert counts, timestamps, etc.).
         */
        Map<String, Object> metadata
) {
}