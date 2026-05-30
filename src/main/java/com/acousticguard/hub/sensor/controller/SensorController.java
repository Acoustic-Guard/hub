package com.acousticguard.hub.sensor.controller;

import com.acousticguard.hub.common.enums.SensorStatus;
import com.acousticguard.hub.sensor.model.Sensor;
import com.acousticguard.hub.sensor.service.SensorMonitorService;
import com.acousticguard.hub.telemetry.dto.SensorNodeResponseDto;
import com.acousticguard.hub.telemetry.service.TelemetryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for sensor management.
 * Provides endpoints for sensor telemetry and status.
 */
@RestController
@RequestMapping("/api/v1/sensors")
@RequiredArgsConstructor
public class SensorController {

    private final SensorMonitorService sensorMonitorService;
    private final TelemetryService telemetryService;

    /**
     * Gets all sensor nodes with their status.
     * 
     * @return list of sensors with status
     */
    @GetMapping
    public ResponseEntity<List<SensorNodeResponseDto>> getAllNodes() {
        List<Sensor> sensors = sensorMonitorService.getAllSensors();
        
        List<SensorNodeResponseDto> nodes = sensors.stream()
                .map(sensor -> new SensorNodeResponseDto(
                        sensor.getId(),
                        sensor.getLocation(),
                        sensor.getStatus(),
                        null, // latencyMs
                        null, // uptimePercent
                        sensor.getLastHeartbeat(),
                        sensor.getLatitude(),
                        sensor.getLongitude(),
                        null, // firmwareVersion
                        null  // metadata
                ))
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(nodes);
    }
}
