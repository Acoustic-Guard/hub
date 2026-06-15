package com.acousticguard.hub.common.dto;

import java.time.Instant;

/**
 * Data Transfer Object for error responses.
 * <p>
 * Contains error information returned to API clients when an exception occurs.
 * Includes a human-readable error message, error type classification, and
 * timestamp. Provides a standardized error format across all API endpoints.
 * </p>
 */
public record ErrorResponseDto(
        /**
         * Human-readable error message describing what went wrong.
         */
        String message,
        /**
         * The type or category of error (e.g., "ValidationError", "NotFoundError").
         */
        String errorType,
        /**
         * The timestamp when the error occurred.
         */
        Instant timestamp
) {
    /**
     * Convenience constructor that sets the timestamp to the current time.
     *
     * @param message   the error message
     * @param errorType the error type
     */
    public ErrorResponseDto(String message, String errorType) {
        this(message, errorType, Instant.now());
    }
}
