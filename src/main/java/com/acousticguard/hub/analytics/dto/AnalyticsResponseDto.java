package com.acousticguard.hub.analytics.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AnalyticsResponseDto(
        Long totalIncidents,
        Long activeAlerts,
        Double avgConfidence,
        Long criticalCount,
        List<ThreatDistributionDto> threatDistribution,
        List<IncidentHistoryDto> history
) {
}
