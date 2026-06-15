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
 * Provides endpoints for retrieving and updating incidents.
 */
@RestController
@RequestMapping("/api/v1/incidents")
@RequiredArgsConstructor
@Tag(name = "Incidents", description = "Incident management endpoints")
public class IncidentController {

    private final IncidentService incidentService;
    private final IncidentMapper incidentMapper;

    // GET /api/v1/incidents
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
     * Retrieves a specific incident by ID.
     *
     * @param id the incident identifier
     * @return the incident if found
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
     * Updates the status of an incident.
     *
     * @param id     the incident identifier
     * @param status the new status
     * @return the updated incident
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
