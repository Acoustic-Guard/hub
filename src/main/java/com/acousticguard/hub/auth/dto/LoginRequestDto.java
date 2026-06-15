package com.acousticguard.hub.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Data Transfer Object for login requests.
 * <p>
 * Contains user credentials for authentication. Used in POST requests to
 * the login endpoint to obtain an authentication token. Both username and
 * password are required fields validated by Jakarta Bean Validation.
 * </p>
 */
public record LoginRequestDto(
        /**
         * The username for authentication.
         */
        @NotBlank(message = "Username is required")
        String username,

        /**
         * The password for authentication.
         */
        @NotBlank(message = "Password is required")
        String password
) {
}