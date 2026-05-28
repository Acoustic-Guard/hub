package com.acousticguard.hub.alert.dto;

import jakarta.validation.constraints.NotBlank;

public record AlertStatusUpdateDto(
        @NotBlank(message = "Status cannot be blank")
        String status
) {
}