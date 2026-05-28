package com.acousticguard.hub.telemetry.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TelemetryResponseDto(
        Integer activeNodes,
        Integer avgLatencyMs,
        Integer noiseLevelDb,
        String nodesStatus,
        String latencyStatus,
        String noiseStatus,

        Integer offlineNodes,
        Integer warningNodes,
        Float uptimePercent,
        Instant lastUpdatedAt
) {
}