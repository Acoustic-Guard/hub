package com.acousticguard.hub.telemetry.dto;

import com.acousticguard.hub.common.enums.SystemStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TelemetryResponseDto(
        Integer activeNodes,
        Integer avgLatencyMs,
        Integer noiseLevelDb,
        SystemStatus nodesStatus,
        SystemStatus latencyStatus,
        SystemStatus noiseStatus,

        Integer offlineNodes,
        Integer warningNodes,
        Float uptimePercent,
        Instant lastUpdatedAt
) {}