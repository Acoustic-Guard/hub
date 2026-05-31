package com.acousticguard.hub.alert.dto;

import com.acousticguard.hub.common.enums.ThreatType;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AlertResponseDto(
        UUID id,
        ThreatType threatType,
        Float confidence,
        String location,
        Instant detectedAt,

        String sensorId,
        Float latitude,
        Float longitude,
        Map<String, Object> metadata
) {
}