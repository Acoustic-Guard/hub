package com.acousticguard.hub.telemetry.controller;

import com.acousticguard.hub.telemetry.dto.TelemetryResponseDto;
import com.acousticguard.hub.telemetry.service.TelemetryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for system telemetry.
 * Provides endpoints for system-wide telemetry data.
 */
@RestController
@RequestMapping("/api/v1/telemetry")
@RequiredArgsConstructor
public class TelemetryController {

    private final TelemetryService telemetryService;

    /**
     * Gets current system telemetry.
     *
     * @return the current system telemetry
     */
    @GetMapping
    public ResponseEntity<TelemetryResponseDto> getSystemTelemetry() {
        TelemetryResponseDto telemetry = telemetryService.getSystemTelemetry();
        return ResponseEntity.ok(telemetry);
    }
}
