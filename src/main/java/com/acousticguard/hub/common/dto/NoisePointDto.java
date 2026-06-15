package com.acousticguard.hub.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Data Transfer Object for noise data points.
 * <p>
 * Contains geographic location and decibel level for a single noise measurement.
 * Used to represent noise map data for visualization and analytics. Null values
 * are excluded from JSON serialization to reduce payload size.
 * </p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record NoisePointDto(
        /**
         * GPS latitude coordinate of the noise measurement.
         */
        Float latitude,
        /**
         * GPS longitude coordinate of the noise measurement.
         */
        Float longitude,
        /**
         * Decibel level at the measurement location.
         */
        Float db
) {
}
