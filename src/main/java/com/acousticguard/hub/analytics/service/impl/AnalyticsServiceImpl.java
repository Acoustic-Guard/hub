package com.acousticguard.hub.analytics.service.impl;

import com.acousticguard.hub.analytics.dto.AnalyticsResponseDto;
import com.acousticguard.hub.analytics.dto.IncidentHistoryDto;
import com.acousticguard.hub.analytics.dto.ThreatDistributionDto;
import com.acousticguard.hub.analytics.dto.TimeSeriesPointDto;
import com.acousticguard.hub.analytics.service.AnalyticsService;
import com.acousticguard.hub.alert.repository.AlertRepository;
import com.acousticguard.hub.common.enums.IncidentStatus;
import com.acousticguard.hub.common.enums.ThreatType;
import com.acousticguard.hub.incident.model.Incident;
import com.acousticguard.hub.incident.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsServiceImpl implements AnalyticsService {

    private final IncidentRepository incidentRepository;
    private final AlertRepository alertRepository;

    @Override
    public AnalyticsResponseDto getAnalytics(String range, String start, String end) {
        Instant threshold;
        Instant endTime;
        
        if (start != null && end != null && !start.isBlank() && !end.isBlank()) {
            threshold = Instant.parse(start);
            endTime = Instant.parse(end);
        } else {
            threshold = parseRangeToThreshold(range);
            endTime = Instant.now();
        }

        long totalIncidents = incidentRepository.countByCreatedAtAfter(threshold);
        long activeAlerts = alertRepository.countByDetectedAtAfter(threshold);
        
        Double avgIntensity = incidentRepository.averageIntensityByCreatedAtAfter(threshold);
        double avgConfidence = avgIntensity != null ? avgIntensity * 100.0 : 0.0;
        
        long criticalCount = incidentRepository.countByCreatedAtAfterAndIntensityGreaterThanEqual(threshold, 0.9f);
        
        List<ThreatDistributionDto> threatDistribution = incidentRepository.findThreatDistributionByCreatedAtAfter(threshold);
        
        List<Incident> recentIncidents = incidentRepository.findTop50ByCreatedAtAfterOrderByCreatedAtDesc(threshold);
        List<IncidentHistoryDto> history = recentIncidents.stream()
                .map(this::mapToIncidentHistoryDto)
                .toList();

        List<TimeSeriesPointDto> timeSeries = buildTimeSeries(threshold, endTime);

        return new AnalyticsResponseDto(
                totalIncidents,
                activeAlerts,
                avgConfidence,
                criticalCount,
                threatDistribution,
                history,
                timeSeries
        );
    }

    private Instant parseRangeToThreshold(String range) {
        return switch (range.toLowerCase()) {
            case "7d" -> Instant.now().minus(7, ChronoUnit.DAYS);
            case "30d" -> Instant.now().minus(30, ChronoUnit.DAYS);
            default -> Instant.now().minus(1, ChronoUnit.DAYS);
        };
    }

    private IncidentHistoryDto mapToIncidentHistoryDto(Incident incident) {
        ThreatType threatType = parseThreatType(incident.getType());
        IncidentStatus status = parseIncidentStatus(incident.getStatus());
        
        Point locationGeo = incident.getLocationGeo();
        Float latitude = locationGeo != null ? (float) locationGeo.getY() : null;
        Float longitude = locationGeo != null ? (float) locationGeo.getX() : null;
        
        return new IncidentHistoryDto(
                incident.getId(),
                threatType,
                incident.getIntensity(),
                status,
                incident.getCreatedAt(),
                latitude,
                longitude
        );
    }

    private List<TimeSeriesPointDto> buildTimeSeries(Instant threshold, Instant endTime) {
        long durationHours = java.time.Duration.between(threshold, endTime).toHours();
        ChronoUnit stepUnit = durationHours <= 24 ? ChronoUnit.HOURS : ChronoUnit.DAYS;

        Instant roundedEndTime = endTime.truncatedTo(stepUnit);

        List<Instant> bucketStarts = new ArrayList<>();
        Instant current = threshold.truncatedTo(stepUnit);
        while (current.isBefore(roundedEndTime) || current.equals(roundedEndTime)) {
            bucketStarts.add(current);
            current = current.plus(1, stepUnit);
        }

        List<Incident> allIncidents = incidentRepository.findByCreatedAtAfterOrderByCreatedAtAsc(threshold);

        return bucketStarts.stream()
                .map(bucketStart -> {
                    Instant bucketEnd = bucketStart.plus(1, stepUnit);

                    Map<ThreatType, Long> typeCounts = allIncidents.stream()
                            .filter(incident -> !incident.getCreatedAt().isBefore(bucketStart)
                                    && incident.getCreatedAt().isBefore(bucketEnd))
                            .collect(Collectors.groupingBy(
                                    incident -> parseThreatType(incident.getType()),
                                    Collectors.counting()
                            ));

                    Map<String, Long> flattenedCounts = Arrays.stream(ThreatType.values())
                            .filter(type -> type != ThreatType.BACKGROUND)
                            .collect(Collectors.toMap(
                                    ThreatType::getValue,
                                    type -> typeCounts.getOrDefault(type, 0L)
                            ));

                    return new TimeSeriesPointDto(bucketStart, flattenedCounts);
                })
                .toList();
    }

    private ThreatType parseThreatType(String type) {
        if (type == null) {
            return ThreatType.BACKGROUND;
        }
        try {
            return ThreatType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown threat type: {}", type);
            return ThreatType.BACKGROUND;
        }
    }

    private IncidentStatus parseIncidentStatus(String status) {
        if (status == null) {
            return IncidentStatus.DETECTED;
        }
        try {
            return IncidentStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown incident status: {}", status);
            return IncidentStatus.DETECTED;
        }
    }
}
