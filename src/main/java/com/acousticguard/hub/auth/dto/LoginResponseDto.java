package com.acousticguard.hub.auth.dto;

import com.acousticguard.hub.common.enums.UserRole;

/**
 * Data Transfer Object for login responses.
 * <p>
 * Contains the authentication token and user information returned after
 * successful authentication. The token is a JWT (JSON Web Token) that
 * must be included in the Authorization header for subsequent API requests.
 * </p>
 */
public record LoginResponseDto(
        /**
         * The JWT authentication token.
         */
        String token,
        /**
         * The username of the authenticated user.
         */
        String username,
        /**
         * The role of the authenticated user (e.g., ADMIN, OPERATOR).
         */
        UserRole role
) {
}