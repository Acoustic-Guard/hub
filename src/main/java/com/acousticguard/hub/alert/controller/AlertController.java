package com.acousticguard.hub.alert.controller;

import com.acousticguard.hub.alert.dto.AlertResponseDto;
import com.acousticguard.hub.alert.dto.AlertStatusUpdateDto;
import com.acousticguard.hub.alert.mapper.AlertMapper;
import com.acousticguard.hub.alert.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for alert management.
 * Provides endpoints for retrieving and updating alerts.
 */
@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;
    private final AlertMapper alertMapper;

    /**
     * Retrieves all alerts with pagination support.
     *
     * @param pageable pagination parameters
     * @return paginated list of alerts
     */
    @GetMapping
    public ResponseEntity<Page<AlertResponseDto>> getAllAlerts(Pageable pageable) {
        // TODO: Implement pagination in AlertService
        return ResponseEntity.ok().build();
    }

    /**
     * Retrieves a specific alert by ID.
     *
     * @param id the alert identifier
     * @return the alert if found
     */
    @GetMapping("/{id}")
    public ResponseEntity<AlertResponseDto> getAlertById(@PathVariable UUID id) {
        return alertService.findById(id)
                .map(alertMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Updates the status of an alert.
     *
     * @param id           the alert identifier
     * @param statusUpdate the status update request
     * @return the updated alert
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
