package com.acousticguard.hub.analytics.dto;

import com.acousticguard.hub.common.enums.IncidentStatus;
import com.acousticguard.hub.common.enums.ThreatType;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.UUID;

/**
 * Data Transfer Object for incident history data.
 * <p>
 * Contains historical incident information for trend analysis and reporting.
 * This DTO is used in analytics responses to provide incident history over time.
 * Null values are excluded from JSON serialization to reduce payload size.
 * </p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record IncidentHistoryDto(
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
