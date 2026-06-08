package com.acousticguard.hub.incident.service.impl;

import com.acousticguard.hub.alert.model.Alert;
import com.acousticguard.hub.common.enums.IncidentStatus;
import com.acousticguard.hub.common.error.IncidentNotFoundError;
import com.acousticguard.hub.incident.mapper.IncidentMapper;
import com.acousticguard.hub.incident.model.Incident;
import com.acousticguard.hub.incident.repository.IncidentRepository;
import com.acousticguard.hub.incident.service.IncidentService;
import com.acousticguard.hub.websocket.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of IncidentService.
 * Handles incident creation, aggregation, and status management.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IncidentServiceImpl implements IncidentService {

    private final IncidentRepository incidentRepository;
    private final EventPublisher eventPublisher;
    private final IncidentMapper incidentMapper;

    @Value("${acoustic.incident.spatial-threshold-meters:500}")
    private double spatialThresholdMeters;

    @Value("${acoustic.incident.temporal-threshold-seconds:300}")
    private long temporalThresholdSeconds;

    @Override
    @Transactional
    public Incident aggregateOrUpdate(Alert alert) {
        List<Incident> nearbyIncidents = findNearbyIncidents(alert);

        if (!nearbyIncidents.isEmpty()) {
            Incident existing = nearbyIncidents.get(0);
            return updateIncidentWithAlert(existing, alert);
        }

        float latitude = (float) alert.getLocationGeo().getY();
        float longitude = (float) alert.getLocationGeo().getX();
        Point location = incidentMapper.latitudeLongitudeToPoint(latitude, longitude);

        Incident newIncident = Incident.builder()
                .locationGeo(location)
                .type(alert.getThreatType())
                .intensity(alert.getConfidence())
                .status(IncidentStatus.DETECTED.getValue())
                .sensorId(alert.getSensorId())
                .metadata(buildInitialMetadata(alert))
                .build();

        Incident saved = incidentRepository.save(newIncident);
        log.info("Created new incident {} for alert {} at location {},{}",
                saved.getId(), alert.getId(), latitude, longitude);

        eventPublisher.publishIncident(incidentMapper.toDto(saved));

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Incident> findAllActive() {
        return incidentRepository.findByStatusNot(IncidentStatus.RESOLVED.getValue());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Incident> findById(UUID id) {
        return incidentRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Incident> findActiveWithinBbox(float minLat, float maxLat, float minLng, float maxLng) {
        return incidentRepository.findActiveWithinBbox(minLng, minLat, maxLng, maxLat);
    }

    @Override
    @Transactional
    public Incident updateStatus(UUID id, String status) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new IncidentNotFoundError(id));

        incident.setStatus(status);
        Incident updated = incidentRepository.save(incident);
        log.info("Updated incident {} status to {}", id, status);

        eventPublisher.publishIncident(incidentMapper.toDto(updated));

        return updated;
    }

    private List<Incident> findNearbyIncidents(Alert alert) {
        Instant timeThreshold = Instant.now().minusSeconds(temporalThresholdSeconds);

        return incidentRepository.findNearbyActiveIncidents(
                alert.getLocationGeo().getY(),
                alert.getLocationGeo().getX(),
                spatialThresholdMeters,
                alert.getThreatType(),
                timeThreshold
        );
    }

    private Incident updateIncidentWithAlert(Incident incident, Alert alert) {
        float newIntensity = Math.max(incident.getIntensity(), alert.getConfidence());
        incident.setIntensity(newIntensity);

        if (newIntensity >= 0.9f) {
            incident.setStatus(IncidentStatus.CONFIRMED.getValue());
        } else if (newIntensity >= 0.8f) {
            incident.setStatus(IncidentStatus.INVESTIGATING.getValue());
        }

        incident.getMetadata().put("lastAlertId", alert.getId().toString());
        incident.getMetadata().put("lastAlertAt", alert.getDetectedAt().toString());
        incident.getMetadata().put("alertCount", ((Integer) incident.getMetadata().getOrDefault("alertCount", 0)) + 1);

        Incident updated = incidentRepository.save(incident);
        log.info("Updated incident {} with alert {}, new intensity: {}",
                updated.getId(), alert.getId(), newIntensity);

        eventPublisher.publishIncident(incidentMapper.toDto(updated));

        return updated;
    }

    private Map<String, Object> buildInitialMetadata(Alert alert) {
        return Map.of(
                "firstAlertId", alert.getId().toString(),
                "firstAlertAt", alert.getDetectedAt().toString(),
                "alertCount", 1
        );
    }
}
