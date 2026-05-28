package com.acousticguard.hub.incident.dto;

import com.acousticguard.hub.common.enums.IncidentStatus;
import com.acousticguard.hub.common.enums.ThreatType;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record IncidentResponseDto(
        UUID id,
        Float latitude,
        Float longitude,
        ThreatType type,
        Float intensity,

        IncidentStatus status,
        String sensorId,
        Instant createdAt,
        Instant updatedAt,
        Map<String, Object> metadata
) {}