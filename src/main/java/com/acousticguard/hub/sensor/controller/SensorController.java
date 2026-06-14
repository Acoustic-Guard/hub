package com.acousticguard.hub.sensor.controller;

import com.acousticguard.hub.sensor.mapper.SensorMapper;
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

/**
 * REST controller for sensor management.
 * Provides endpoints for sensor telemetry and status.
 */
@RestController
@RequestMapping("/api/v1/sensors")
@RequiredArgsConstructor
public class SensorController {

    private final SensorMonitorService sensorMonitorService;
    private final SensorMapper sensorMapper;
    private final TelemetryService telemetryService;

    /**
     * Gets all sensor nodes with their status and latency.
     *
     * @return list of sensors with status and latency
     */
    @GetMapping
    public ResponseEntity<List<SensorNodeResponseDto>> getAllNodes() {
        List<Sensor> sensors = sensorMonitorService.getAllSensors();
        List<SensorNodeResponseDto> nodes = sensors.stream()
                .map(sensor -> {
                    var dto = sensorMapper.toDto(sensor);
                    // Add latency from telemetry if available
                    Long latency = telemetryService.getSensorLatency(sensor.getId());
                    if (latency != null) {
                        dto = new SensorNodeResponseDto(
                            dto.id(),
                            dto.location(),
                            dto.status(),
                            latency.intValue(),
                            dto.uptimePercent(),
                            dto.lastHeartbeat(),
                            dto.latitude(),
                            dto.longitude(),
                            dto.firmwareVersion(),
                            dto.metadata()
                        );
                    }
                    return dto;
                })
                .toList();
        return ResponseEntity.ok(nodes);
    }
}
