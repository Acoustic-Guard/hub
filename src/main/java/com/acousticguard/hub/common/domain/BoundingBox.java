package com.acousticguard.hub.common.domain;

import java.util.Objects;

/**
 * Domain value object representing a geographic bounding box.
 * Format: minLng, minLat, maxLng, maxLat
 */
public record BoundingBox(float minLng, float minLat, float maxLng, float maxLat) {

    /**
     * Parses a bounding box string in format "minLng,minLat,maxLng,maxLat".
     *
     * @param bbox the bounding box string
     * @return parsed BoundingBox
     * @throws IllegalArgumentException if format is invalid or coordinates are invalid
     */
    public static BoundingBox fromString(String bbox) {
        if (bbox == null || bbox.isBlank()) {
            throw new IllegalArgumentException("Bounding box cannot be null or blank");
        }

        String[] coords = bbox.split(",");
        if (coords.length != 4) {
            throw new IllegalArgumentException("Bounding box must have exactly 4 coordinates in format 'minLng,minLat,maxLng,maxLat'");
        }

        try {
            float minLng = Float.parseFloat(coords[0].trim());
            float minLat = Float.parseFloat(coords[1].trim());
            float maxLng = Float.parseFloat(coords[2].trim());
            float maxLat = Float.parseFloat(coords[3].trim());

            validateCoordinates(minLng, minLat, maxLng, maxLat);

            return new BoundingBox(minLng, minLat, maxLng, maxLat);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Bounding box coordinates must be valid numbers", e);
        }
    }

    private static void validateCoordinates(float minLng, float minLat, float maxLng, float maxLat) {
        if (minLng < -180 || minLng > 180) {
            throw new IllegalArgumentException("minLng must be between -180 and 180");
        }
        if (minLat < -90 || minLat > 90) {
            throw new IllegalArgumentException("minLat must be between -90 and 90");
        }
        if (maxLng < -180 || maxLng > 180) {
            throw new IllegalArgumentException("maxLng must be between -180 and 180");
        }
        if (maxLat < -90 || maxLat > 90) {
            throw new IllegalArgumentException("maxLat must be between -90 and 90");
        }
        if (minLng > maxLng) {
            throw new IllegalArgumentException("minLng must be less than or equal to maxLng");
        }
        if (minLat > maxLat) {
            throw new IllegalArgumentException("minLat must be less than or equal to maxLat");
        }
    }

    /**
     * Gets the minimum longitude.
     */
    public float minLng() {
        return minLng;
    }

    /**
     * Gets the minimum latitude.
     */
    public float minLat() {
        return minLat;
    }

    /**
     * Gets the maximum longitude.
     */
    public float maxLng() {
        return maxLng;
    }

    /**
     * Gets the maximum latitude.
     */
    public float maxLat() {
        return maxLat;
    }
}
