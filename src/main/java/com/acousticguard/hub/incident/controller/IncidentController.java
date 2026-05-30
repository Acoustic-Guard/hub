package com.acousticguard.hub.incident.controller;

import com.acousticguard.hub.incident.dto.IncidentResponseDto;
import com.acousticguard.hub.incident.mapper.IncidentMapper;
import com.acousticguard.hub.incident.service.IncidentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for incident management.
 * Provides endpoints for retrieving and updating incidents.
 */
@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentService incidentService;
    private final IncidentMapper incidentMapper;

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
     * @param id the incident identifier
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
