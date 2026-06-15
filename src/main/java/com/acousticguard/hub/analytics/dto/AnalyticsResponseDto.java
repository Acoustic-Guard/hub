package com.acousticguard.hub.analytics.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Data Transfer Object for analytics responses.
 * <p>
 * Contains aggregated analytics data including incident counts, alert statistics,
 * threat distribution, and time series data. This DTO is used to serialize
 * analytics data for REST API responses to dashboard clients. Null values are
 * excluded from JSON serialization to reduce payload size.
 * </p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AnalyticsResponseDto(
        /**
         * Total number of incidents in the system.
         */
        Long totalIncidents,
        /**
         * Number of currently active alerts.
         */
        Long activeAlerts,
        /**
         * Average confidence score across all alerts.
         */
        Double avgConfidence,
        /**
         * Number of critical incidents (high intensity).
         */
        Long criticalCount,
        /**
         * Distribution of incidents by threat type.
         */
        List<ThreatDistributionDto> threatDistribution,
        /**
         * Historical incident data for trend analysis.
         */
        List<IncidentHistoryDto> history,
        /**
         * Time series data for incident trends over time.
         */
        List<TimeSeriesPointDto> timeSeries
) {
}
