package com.acousticguard.hub.analytics.service.impl;

import com.acousticguard.hub.analytics.dto.AnalyticsResponseDto;
import com.acousticguard.hub.analytics.dto.IncidentHistoryDto;
import com.acousticguard.hub.analytics.dto.ThreatDistributionDto;
import com.acousticguard.hub.analytics.service.AnalyticsService;
import com.acousticguard.hub.alert.repository.AlertRepository;
import com.acousticguard.hub.common.enums.IncidentStatus;
import com.acousticguard.hub.common.enums.ThreatType;
import com.acousticguard.hub.incident.model.Incident;
import com.acousticguard.hub.incident.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsServiceImpl implements AnalyticsService {

    private final IncidentRepository incidentRepository;
    private final AlertRepository alertRepository;

    @Override
    public AnalyticsResponseDto getAnalytics(String range) {
        Instant threshold = parseRangeToThreshold(range);

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

        return new AnalyticsResponseDto(
                totalIncidents,
                activeAlerts,
                avgConfidence,
                criticalCount,
                threatDistribution,
                history
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
        
        return new IncidentHistoryDto(
                incident.getId(),
                threatType,
                incident.getIntensity(),
                status,
                incident.getCreatedAt()
        );
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
