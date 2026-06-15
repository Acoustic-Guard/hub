package com.acousticguard.hub.alert.dto;

import com.acousticguard.hub.common.enums.IncidentStatus;
import com.acousticguard.hub.common.validation.ValueOfEnum;
import jakarta.validation.constraints.NotBlank;

/**
 * Data Transfer Object for alert status update requests.
 * <p>
 * Contains the new status value for an alert. Used in PATCH requests to
 * update alert status (e.g., from PENDING to RESOLVED). The status field
 * is validated to ensure it matches a valid IncidentStatus enum value.
 * </p>
 */
public record AlertStatusUpdateDto(
        /**
         * The new status value for the alert.
         * Must be a valid IncidentStatus enum value (e.g., "RESOLVED", "ACKNOWLEDGED").
         */
        @NotBlank(message = "Status cannot be blank")
        @ValueOfEnum(enumClass = IncidentStatus.class, message = "Invalid status value")
        String status
) {
}