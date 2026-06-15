package com.acousticguard.hub.analytics.dto;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

/**
 * Data Transfer Object for time series data points.
 * <p>
 * Contains a timestamp and a map of counts for different categories.
 * Used in analytics responses to provide time series data for trend visualization.
 * The counts map uses @JsonAnyGetter to flatten the map into top-level JSON properties.
 * Null values are excluded from JSON serialization to reduce payload size.
 * </p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TimeSeriesPointDto(
        /**
         * The timestamp for this data point.
         * Formatted as ISO 8601 string in UTC timezone.
         */
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        Instant timestamp,

        /**
         * A map of category names to their counts at this timestamp.
         * Flattened to top-level JSON properties via @JsonAnyGetter.
         */
        @JsonAnyGetter
        Map<String, Long> counts
) {
}