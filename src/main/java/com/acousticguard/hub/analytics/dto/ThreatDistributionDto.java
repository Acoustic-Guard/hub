package com.acousticguard.hub.analytics.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Data Transfer Object for threat distribution data.
 * <p>
 * Contains the name of a threat type and its occurrence count.
 * Used in analytics dashboards to display the distribution of threats
 * by type (e.g., gunshot vs. scream incidents). Null values are excluded
 * from JSON serialization to reduce payload size.
 * </p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ThreatDistributionDto(
        /**
         * The name of the threat type (e.g., "GUNSHOT", "SCREAM").
         */
        String name,
        /**
         * The count of incidents of this threat type.
         */
        Long value
) {
}
