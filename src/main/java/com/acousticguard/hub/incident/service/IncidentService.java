package com.acousticguard.hub.incident.service;

import com.acousticguard.hub.alert.model.Alert;
import com.acousticguard.hub.incident.model.Incident;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service interface for incident management.
 * Defines business operations for creating, aggregating, and managing incidents.
 */
public interface IncidentService {

    /**
     * Creates or updates an incident based on an alert.
     * Aggregates alerts into incidents based on spatial and temporal proximity.
     *
     * @param alert the alert to aggregate into an incident
     * @return the created or updated incident
     */
    Incident aggregateOrUpdate(Alert alert);

    /**
     * Retrieves an incident by its identifier.
     *
     * @param id the incident identifier
     * @return the incident if found
     */
    Optional<Incident> findById(UUID id);

    /**
     * Finds active incidents within a bounding box.
     *
     * @param minLat minimum latitude
     * @param maxLat maximum latitude
     * @param minLng minimum longitude
     * @param maxLng maximum longitude
     * @return list of active incidents within the bounding box
     */
    List<Incident> findActiveWithinBbox(float minLat, float maxLat, float minLng, float maxLng);

    /**
     * Updates the status of an incident.
     *
     * @param id     the incident identifier
     * @param status the new status
     * @return the updated incident
     */
    Incident updateStatus(UUID id, String status);
}
