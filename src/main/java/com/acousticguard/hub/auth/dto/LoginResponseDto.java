package com.acousticguard.hub.auth.dto;

import com.acousticguard.hub.common.enums.UserRole;

public record LoginResponseDto(
        String token,
        String username,
        UserRole role
) {
}