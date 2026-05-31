package com.acousticguard.hub.alert.dto;

import com.acousticguard.hub.common.enums.IncidentStatus;
import com.acousticguard.hub.common.validation.ValueOfEnum;
import jakarta.validation.constraints.NotBlank;

public record AlertStatusUpdateDto(
        @NotBlank(message = "Status cannot be blank")
        @ValueOfEnum(enumClass = IncidentStatus.class, message = "Invalid status value")
        String status
) {
}