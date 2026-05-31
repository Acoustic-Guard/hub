package com.acousticguard.hub.telemetry.dto;

import com.acousticguard.hub.common.enums.SensorStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SensorNodeResponseDto(
        String id,
        String location,
        SensorStatus status,
        Integer latencyMs,
        Float uptimePercent,
        Instant lastHeartbeat,


        Float latitude,
        Float longitude,
        String firmwareVersion,
        Map<String, Object> metadata
) {
}