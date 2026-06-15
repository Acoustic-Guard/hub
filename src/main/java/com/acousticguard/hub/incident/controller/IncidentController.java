package com.acousticguard.hub.incident.controller;

import com.acousticguard.hub.common.domain.BoundingBox;
import com.acousticguard.hub.incident.dto.IncidentResponseDto;
import com.acousticguard.hub.incident.mapper.IncidentMapper;
import com.acousticguard.hub.incident.service.IncidentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for incident management.
 * <p>
 * Provides endpoints for retrieving active incidents, filtering by geographic bounding box,
 * and updating incident status. This controller delegates business logic to {@link IncidentService}
 * and uses {@link IncidentMapper} for DTO conversions. All incident queries leverage PostGIS
 * spatial capabilities for efficient geospatial filtering.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/incidents")
@RequiredArgsConstructor
@Tag(name = "Incidents", description = "Incident management endpoints")
public class IncidentController {

    private final IncidentService incidentService;
    private final IncidentMapper incidentMapper;

    /**
     * Retrieves all active incidents, optionally filtered by a geographic bounding box.
     * <p>
     * If no bounding box parameter is provided, returns all active incidents in the system.
     * When a bounding box is specified, uses PostGIS spatial queries to filter incidents
     * within the specified geographic area. The bounding box format is "minLng,minLat,maxLng,maxLat".
     * </p>
     *
     * @param bbox optional bounding box string in format "minLng,minLat,maxLng,maxLat"
     * @return HTTP 200 OK with a list of active incidents (filtered by bbox if provided),
     * HTTP 400 Bad Request if bbox format is invalid,
     * or HTTP 500 if an internal server error occurs
     */
    @GetMapping
    @Operation(summary = "Get active incidents", description = "Retrieve all active incidents, optionally filtered by bounding box")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved incidents"),
            @ApiResponse(responseCode = "400", description = "Invalid bounding box format")
    })
    public ResponseEntity<List<IncidentResponseDto>> getActiveIncidents(
            @Parameter(description = "Bounding box in format 'minLng,minLat,maxLng,maxLat'")
            @RequestParam(required = false) String bbox) {

        if (bbox == null || bbox.isBlank()) {
            List<IncidentResponseDto> allIncidents = incidentService.findAllActive()
                    .stream()
                    .map(incidentMapper::toDto)
                    .toList();
            return ResponseEntity.ok(allIncidents);
        }

        try {
            BoundingBox boundingBox = BoundingBox.fromString(bbox);
            List<IncidentResponseDto> bboxIncidents = incidentService.findActiveWithinBbox(
                            boundingBox.minLat(), boundingBox.maxLat(),
                            boundingBox.minLng(), boundingBox.maxLng()
                    )
                    .stream()
                    .map(incidentMapper::toDto)
                    .toList();

            return ResponseEntity.ok(bboxIncidents);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Retrieves a specific incident by its unique identifier.
     * <p>
     * This endpoint returns detailed information about a single incident including
     * its status, location, associated alerts, and metadata. If the incident does not
     * exist or is not found, returns HTTP 404.
     * </p>
     *
     * @param id the unique identifier (UUID) of the incident to retrieve
     * @return HTTP 200 OK with the incident details if found,
     * HTTP 404 Not Found if the incident does not exist,
     * or HTTP 500 if an internal server error occurs
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get incident by ID", description = "Retrieve a specific incident by its UUID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved incident"),
            @ApiResponse(responseCode = "404", description = "Incident not found")
    })
    public ResponseEntity<IncidentResponseDto> getIncidentById(
            @Parameter(description = "Incident UUID")
            @PathVariable UUID id) {
        return incidentService.findById(id)
                .map(incidentMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Updates the status of a specific incident.
     * <p>
     * This endpoint allows changing the incident status (e.g., from ACTIVE to RESOLVED,
     * INVESTIGATING, etc.). The status update is persisted to the database and triggers
     * any necessary downstream notifications. Invalid status values will result in HTTP 404.
     * </p>
     *
     * @param id     the unique identifier (UUID) of the incident to update
     * @param status the new status value as a string (e.g., "RESOLVED", "INVESTIGATING")
     * @return HTTP 200 OK with the updated incident details if successful,
     * HTTP 404 Not Found if the incident does not exist or status is invalid,
     * or HTTP 500 if an internal server error occurs
     */
    @PatchMapping("/{id}/status")
    @Operation(summary = "Update incident status", description = "Update the status of a specific incident")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully updated incident status"),
            @ApiResponse(responseCode = "404", description = "Incident not found")
    })
    public ResponseEntity<IncidentResponseDto> updateIncidentStatus(
            @Parameter(description = "Incident UUID")
            @PathVariable UUID id,
            @Parameter(description = "New status (e.g., RESOLVED, INVESTIGATING)")
            @RequestBody String status) {
        try {
            var updatedIncident = incidentService.updateStatus(id, status);
            return ResponseEntity.ok(incidentMapper.toDto(updatedIncident));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
