package com.acousticguard.hub.auth.dto;

public record LoginResponseDto(
        String token,
        String username,
        String role
) {
}