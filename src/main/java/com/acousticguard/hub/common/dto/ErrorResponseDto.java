package com.acousticguard.hub.common.dto;

import java.time.Instant;

/**
 * DTO for error responses.
 */
public record ErrorResponseDto(
        String message,
        String errorType,
        Instant timestamp
) {
    public ErrorResponseDto(String message, String errorType) {
        this(message, errorType, Instant.now());
    }
}
