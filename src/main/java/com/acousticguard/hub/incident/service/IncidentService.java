package com.acousticguard.hub.incident.service;

import com.acousticguard.hub.alert.model.Alert;
import com.acousticguard.hub.incident.model.Incident;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service interface for incident management.
 * <p>
 * Defines business operations for creating, aggregating, and managing incidents.
 * Incidents are aggregations of alerts that occur within spatial and temporal proximity.
 * This service uses PostGIS spatial queries to efficiently correlate alerts based on
 * geographic location and time windows, enabling intelligent incident detection and tracking.
 * </p>
 */
public interface IncidentService {

    /**
     * Creates or updates an incident based on an alert.
     * <p>
     * This method aggregates alerts into incidents based on spatial and temporal proximity.
     * If an existing incident is found within the configured spatial threshold (default 500 meters)
     * and temporal threshold (default 300 seconds), the alert is added to that incident.
     * Otherwise, a new incident is created. This aggregation logic reduces alert noise
     * by grouping related events into cohesive incidents.
     * </p>
     *
     * @param alert the alert to aggregate into an incident, containing location,
     *              timestamp, and threat classification data
     * @return the created or updated incident entity with the new alert included
     */
    Incident aggregateOrUpdate(Alert alert);

    /**
     * Finds all active incidents in the system.
     * <p>
     * This method retrieves all incidents with status ACTIVE, regardless of geographic location.
     * Active incidents are those that have not been resolved or closed. For production use,
     * consider implementing pagination or filtering parameters to manage large datasets efficiently.
     * </p>
     *
     * @return a list of all active incidents, or an empty list if no active incidents exist
     */
    List<Incident> findAllActive();

    /**
     * Retrieves an incident by its unique identifier.
     * <p>
     * This method queries the database for an incident with the specified UUID.
     * If the incident does not exist, returns an empty Optional.
     * </p>
     *
     * @param id the unique identifier (UUID) of the incident to retrieve
     * @return an Optional containing the incident if found, or empty if not found
     */
    Optional<Incident> findById(UUID id);

    /**
     * Finds active incidents within a geographic bounding box.
     * <p>
     * This method uses PostGIS spatial queries to efficiently filter incidents by
     * geographic area. Only incidents with status ACTIVE are returned. The bounding
     * box is defined by minimum and maximum latitude and longitude coordinates.
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
    List<Incident> findActiveWithinBbox(float minLat, float maxLat, float minLng, float maxLng);

    /**
     * Updates the status of an incident.
     * <p>
     * This method changes the incident status (e.g., from ACTIVE to RESOLVED,
     * INVESTIGATING, etc.). The status update is persisted to the database and
     * may trigger downstream workflows such as notification dispatch or analytics
     * updates. Invalid incident IDs will result in an IllegalArgumentException.
     * </p>
     *
     * @param id     the unique identifier (UUID) of the incident to update
     * @param status the new status value as a string (e.g., "RESOLVED", "INVESTIGATING")
     * @return the updated incident entity with the new status
     * @throws IllegalArgumentException if the incident does not exist or status is invalid
     */
    Incident updateStatus(UUID id, String status);
}
