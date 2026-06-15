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
 * <p>
 * Handles incident creation, aggregation, and status management. This implementation
 * uses PostGIS spatial queries to efficiently correlate alerts into incidents based on
 * geographic proximity and temporal windows. Incidents are automatically promoted to
 * higher severity levels as more alerts are aggregated, providing intelligent
 * incident escalation.
 * </p>
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

    /**
     * Creates or updates an incident based on an alert.
     * <p>
     * This method aggregates alerts into incidents using spatial and temporal proximity.
     * If an existing incident is found within the configured spatial threshold (default 500 meters)
     * and temporal threshold (default 300 seconds), the alert is added to that incident
     * and the incident intensity is updated. Otherwise, a new incident is created.
     * Incident status is automatically escalated based on accumulated intensity.
     * </p>
     *
     * @param alert the alert to aggregate into an incident, containing location,
     *              timestamp, and threat classification data
     * @return the created or updated incident entity
     */
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

    /**
     * Finds all active incidents in the system.
     * <p>
     * This method performs a read-only transaction to retrieve all incidents with
     * status other than RESOLVED. Active incidents include DETECTED, INVESTIGATING,
     * and CONFIRMED states. Results are not sorted; consider adding sorting for production use.
     * </p>
     *
     * @return a list of all active incidents, or an empty list if no active incidents exist
     */
    @Override
    @Transactional(readOnly = true)
    public List<Incident> findAllActive() {
        return incidentRepository.findByStatusNot(IncidentStatus.RESOLVED.getValue());
    }

    /**
     * Retrieves an incident by its unique identifier.
     * <p>
     * This method performs a read-only transaction to query the database for an incident
     * with the specified UUID. If the incident does not exist, returns an empty Optional.
     * </p>
     *
     * @param id the unique identifier (UUID) of the incident to retrieve
     * @return an Optional containing the incident if found, or empty if not found
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<Incident> findById(UUID id) {
        return incidentRepository.findById(id);
    }

    /**
     * Finds active incidents within a geographic bounding box.
     * <p>
     * This method uses PostGIS spatial queries to efficiently filter incidents by
     * geographic area. Only incidents with status other than RESOLVED are returned.
     * The bounding box is defined by minimum and maximum latitude and longitude coordinates.
     * This enables geographic filtering for map-based incident visualization.
     * </p>
     *
     * @param minLat the minimum latitude of the bounding box (south boundary)
     * @param maxLat the maximum latitude of the bounding box (north boundary)
     * @param minLng the minimum longitude of the bounding box (west boundary)
     * @param maxLng the maximum longitude of the bounding box (east boundary)
     * @return a list of active incidents within the specified bounding box,
     * or an empty list if no incidents match the criteria
     */
    @Override
    @Transactional(readOnly = true)
    public List<Incident> findActiveWithinBbox(float minLat, float maxLat, float minLng, float maxLng) {
        return incidentRepository.findActiveWithinBbox(minLng, minLat, maxLng, maxLat);
    }

    /**
     * Updates the status of an incident.
     * <p>
     * This method changes the incident status and persists the update to the database.
     * If the incident does not exist, throws an IncidentNotFoundError. The status update
     * is broadcast via WebSocket for real-time frontend updates.
     * </p>
     *
     * @param id     the unique identifier (UUID) of the incident to update
     * @param status the new status value as a string (e.g., "RESOLVED", "INVESTIGATING")
     * @return the updated incident entity with the new status
     * @throws IncidentNotFoundError if the incident does not exist
     */
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

    /**
     * Finds nearby active incidents for a given alert.
     * <p>
     * This method uses PostGIS spatial queries to find incidents within the configured
     * spatial threshold and temporal window of the alert's location and timestamp.
     * Only incidents with the same threat type are considered for aggregation.
     * </p>
     *
     * @param alert the alert to find nearby incidents for
     * @return a list of nearby active incidents, or an empty list if none found
     */
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

    /**
     * Updates an existing incident with a new alert.
     * <p>
     * This method adds the alert to the incident, updates the incident intensity to the
     * maximum of the current intensity and the new alert's confidence, and escalates the
     * incident status based on the accumulated intensity. Metadata is updated to track
     * the most recent alert and total alert count.
     * </p>
     *
     * @param incident the existing incident to update
     * @param alert    the new alert to add to the incident
     * @return the updated incident entity
     */
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

    /**
     * Builds initial metadata for a new incident.
     *
     * @param alert the first alert associated with the incident
     * @return a map of initial metadata key-value pairs
     */
    private Map<String, Object> buildInitialMetadata(Alert alert) {
        return Map.of(
                "firstAlertId", alert.getId().toString(),
                "firstAlertAt", alert.getDetectedAt().toString(),
                "alertCount", 1
        );
    }
}
