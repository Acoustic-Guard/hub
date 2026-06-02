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
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
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
    private final GeometryFactory geometryFactory = new GeometryFactory();

    @Value("${acoustic.incident.spatial-threshold-meters:500}")
    private double spatialThresholdMeters;

    @Value("${acoustic.incident.temporal-threshold-seconds:300}")
    private long temporalThresholdSeconds;

    @Override
    @Transactional
    public Incident aggregateOrUpdate(Alert alert) {
        // Find existing active incidents within spatial and temporal proximity
        List<Incident> nearbyIncidents = findNearbyIncidents(alert);

        if (!nearbyIncidents.isEmpty()) {
            // Aggregate into existing incident
            Incident existing = nearbyIncidents.get(0);
            return updateIncidentWithAlert(existing, alert);
        }

        // Create new incident with Point location
        Point location = geometryFactory.createPoint(
                new Coordinate(alert.getLocationGeo().getX(), alert.getLocationGeo().getY())
        );

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
                saved.getId(), alert.getId(), saved.getLocationGeo().getY(), saved.getLocationGeo().getX());

        // Publish incident created event
        eventPublisher.publishIncident(incidentMapper.toDto(saved));

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Incident> findAllActive() {
        return incidentRepository.findByStatusNot(com.acousticguard.hub.common.enums.IncidentStatus.RESOLVED.getValue());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Incident> findById(UUID id) {
        return incidentRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Incident> findActiveWithinBbox(float minLat, float maxLat, float minLng, float maxLng) {
        // ST_MakeEnvelope accepts (minX, minY, maxX, maxY) -> (minLng, minLat, maxLng, maxLat)
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

        // Publish incident status updated event
        eventPublisher.publishIncident(incidentMapper.toDto(updated));

        return updated;
    }

    private List<Incident> findNearbyIncidents(Alert alert) {
        // Calculate time threshold
        Instant timeThreshold = Instant.now().minusSeconds(temporalThresholdSeconds);

        // Use PostGIS spatial query to find nearby active incidents
        return incidentRepository.findNearbyActiveIncidents(
                alert.getLocationGeo().getY(),
                alert.getLocationGeo().getX(),
                spatialThresholdMeters,
                alert.getThreatType(),
                timeThreshold
        );
    }

    private Incident updateIncidentWithAlert(Incident incident, Alert alert) {
        // Update intensity to maximum of current and new alert
        float newIntensity = Math.max(incident.getIntensity(), alert.getConfidence());
        incident.setIntensity(newIntensity);

        // Update status based on intensity
        if (newIntensity >= 0.9f) {
            incident.setStatus(IncidentStatus.CONFIRMED.getValue());
        } else if (newIntensity >= 0.8f) {
            incident.setStatus(IncidentStatus.INVESTIGATING.getValue());
        }

        // Update metadata with alert information
        incident.getMetadata().put("lastAlertId", alert.getId().toString());
        incident.getMetadata().put("lastAlertAt", alert.getDetectedAt().toString());
        incident.getMetadata().put("alertCount", ((Integer) incident.getMetadata().getOrDefault("alertCount", 0)) + 1);

        Incident updated = incidentRepository.save(incident);
        log.info("Updated incident {} with alert {}, new intensity: {}",
                updated.getId(), alert.getId(), newIntensity);

        // Publish incident updated event
        eventPublisher.publishIncident(incidentMapper.toDto(updated));

        return updated;
    }

    private java.util.Map<String, Object> buildInitialMetadata(Alert alert) {
        return java.util.Map.of(
                "firstAlertId", alert.getId().toString(),
                "firstAlertAt", alert.getDetectedAt().toString(),
                "alertCount", 1
        );
    }
}
