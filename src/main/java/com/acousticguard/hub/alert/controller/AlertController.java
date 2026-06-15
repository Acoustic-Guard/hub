package com.acousticguard.hub.alert.controller;

import com.acousticguard.hub.alert.dto.AlertResponseDto;
import com.acousticguard.hub.alert.dto.AlertStatusUpdateDto;
import com.acousticguard.hub.alert.mapper.AlertMapper;
import com.acousticguard.hub.alert.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for alert management.
 * <p>
 * Provides endpoints for retrieving all alerts, fetching specific alerts by ID,
 * and updating alert status. Alerts are generated when the threat classification service
 * detects potential threats in audio frames with confidence above the configured threshold.
 * This controller delegates business logic to {@link AlertService} and uses
 * {@link AlertMapper} for DTO conversions.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;
    private final AlertMapper alertMapper;

    /**
     * Retrieves all alerts in the system.
     * <p>
     * This endpoint returns a complete list of all alerts regardless of their status
     * or age. For production use, consider implementing pagination or filtering
     * parameters to manage large datasets efficiently.
     * </p>
     *
     * @return HTTP 200 OK with a list of all alerts,
     * or HTTP 500 if an internal server error occurs
     */
    @GetMapping
    public ResponseEntity<List<AlertResponseDto>> getAllAlerts() {
        List<AlertResponseDto> alerts = alertService.findAll()
                .stream()
                .map(alertMapper::toDto)
                .toList();

        return ResponseEntity.ok(alerts);
    }

    /**
     * Retrieves a specific alert by its unique identifier.
     * <p>
     * This endpoint returns detailed information about a single alert including
     * its associated sensor location, classification confidence, threat type,
     * and current status. If the alert does not exist, returns HTTP 404.
     * </p>
     *
     * @param id the unique identifier (UUID) of the alert to retrieve
     * @return HTTP 200 OK with the alert details if found,
     * HTTP 404 Not Found if the alert does not exist,
     * or HTTP 500 if an internal server error occurs
     */
    @GetMapping("/{id}")
    public ResponseEntity<AlertResponseDto> getAlertById(@PathVariable UUID id) {
        return alertService.findById(id)
                .map(alertMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Updates the status of a specific alert.
     * <p>
     * This endpoint allows changing the alert status (e.g., from PENDING to ACKNOWLEDGED,
     * RESOLVED, etc.). The status update is persisted to the database and may trigger
     * downstream workflows such as incident aggregation or notification dispatch.
     * Invalid alert IDs or status values will result in HTTP 404.
     * </p>
     *
     * @param id           the unique identifier (UUID) of the alert to update
     * @param statusUpdate the DTO containing the new status value
     * @return HTTP 200 OK with the updated alert details if successful,
     * HTTP 404 Not Found if the alert does not exist or status is invalid,
     * or HTTP 500 if an internal server error occurs
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<AlertResponseDto> updateAlertStatus(
            @PathVariable UUID id,
            @RequestBody AlertStatusUpdateDto statusUpdate) {
        try {
            var updatedAlert = alertService.updateStatus(id, statusUpdate.status());
            return ResponseEntity.ok(alertMapper.toDto(updatedAlert));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
