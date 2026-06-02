package com.acousticguard.hub.incident.controller;

import com.acousticguard.hub.incident.dto.IncidentResponseDto;
import com.acousticguard.hub.incident.mapper.IncidentMapper;
import com.acousticguard.hub.incident.service.IncidentService;
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
public class IncidentController {

    private final IncidentService incidentService;
    private final IncidentMapper incidentMapper;

    // GET /api/v1/incidents
    @GetMapping
    public ResponseEntity<List<IncidentResponseDto>> getActiveIncidents(
            @RequestParam(required = false) String bbox) {

        if (bbox == null || bbox.isBlank()) {
            List<IncidentResponseDto> allIncidents = incidentService.findAllActive()
                    .stream()
                    .map(incidentMapper::toDto)
                    .toList();
            return ResponseEntity.ok(allIncidents);
        }

        String[] coords = bbox.split(",");
        if (coords.length != 4) {
            return ResponseEntity.badRequest().build();
        }

        try {
            float minLng = Float.parseFloat(coords[0].trim());
            float minLat = Float.parseFloat(coords[1].trim());
            float maxLng = Float.parseFloat(coords[2].trim());
            float maxLat = Float.parseFloat(coords[3].trim());

            List<IncidentResponseDto> bboxIncidents = incidentService.findActiveWithinBbox(minLat, maxLat, minLng, maxLng)
                    .stream()
                    .map(incidentMapper::toDto)
                    .toList();

            return ResponseEntity.ok(bboxIncidents);

        } catch (NumberFormatException e) {
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
    public ResponseEntity<IncidentResponseDto> getIncidentById(@PathVariable UUID id) {
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
    public ResponseEntity<IncidentResponseDto> updateIncidentStatus(
            @PathVariable UUID id,
            @RequestBody String status) {
        try {
            var updatedIncident = incidentService.updateStatus(id, status);
            return ResponseEntity.ok(incidentMapper.toDto(updatedIncident));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
