package com.acousticguard.hub.incident.dto;

import com.acousticguard.hub.common.enums.IncidentStatus;
import com.acousticguard.hub.common.enums.ThreatType;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.UUID;

/**
 * Data Transfer Object for public incident responses.
 * <p>
 * Contains incident information suitable for public-facing APIs, excluding
 * sensitive metadata. This DTO is used to serialize incident data for
 * REST API responses to external clients. Null values are excluded from
 * JSON serialization to reduce payload size.
 * </p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PublicIncidentResponseDto(
        /**
         * Unique identifier of the incident.
         */
        UUID id,
        /**
         * The type of threat associated with this incident.
         */
        ThreatType type,
        /**
         * The intensity of the incident (maximum confidence of aggregated alerts).
         */
        Float intensity,
        /**
         * The current status of the incident.
         */
        IncidentStatus status,
        /**
         * The timestamp when the incident was first created.
         */
        Instant createdAt,
        /**
         * GPS latitude coordinate of the incident location.
         */
        Float latitude,
        /**
         * GPS longitude coordinate of the incident location.
         */
        Float longitude
) {
}
