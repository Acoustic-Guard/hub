package com.acousticguard.hub.analytics.dto;

import com.acousticguard.hub.common.enums.IncidentStatus;
import com.acousticguard.hub.common.enums.ThreatType;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record IncidentHistoryDto(
        UUID id,
        ThreatType type,
        Float intensity,
        IncidentStatus status,
        Instant createdAt
) {
}
