package com.acousticguard.hub.sensor.controller;

import com.acousticguard.hub.sensor.mapper.SensorMapper;
import com.acousticguard.hub.sensor.model.Sensor;
import com.acousticguard.hub.sensor.service.SensorRegistryService;
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
 * <p>
 * Provides endpoints for retrieving sensor information including status, latency,
 * and telemetry data. This controller delegates sensor registry operations to
 * {@link SensorRegistryService} and telemetry queries to {@link TelemetryService}.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/sensors")
@RequiredArgsConstructor
public class SensorController {

    private final SensorRegistryService sensorRegistryService;
    private final SensorMapper sensorMapper;
    private final TelemetryService telemetryService;

    /**
     * Retrieves all sensor nodes with their current status and network latency.
     * <p>
     * This endpoint fetches all registered sensors from the database, enriches each
     * sensor with real-time latency data from the in-memory telemetry cache, and
     * returns the aggregated results. Latency is calculated as the time difference
     * between when the sensor captured the data and when the hub processed it.
     * </p>
     *
     * @return HTTP 200 OK with a list of sensor nodes including status and latency,
     * or HTTP 500 if an internal server error occurs
     */
    @GetMapping
    public ResponseEntity<List<SensorNodeResponseDto>> getAllNodes() {
        List<Sensor> sensors = sensorRegistryService.getAllSensors();
        List<SensorNodeResponseDto> nodes = sensors.stream()
                .map(sensor -> {
                    var dto = sensorMapper.toDto(sensor);
                    // Add latency from telemetry if available
                    Long latency = telemetryService.getSensorLatency(sensor.getId());
                    if (latency != null) {
                        dto = sensorMapper.updateLatency(dto, latency.intValue());
                    }
                    return dto;
                })
                .toList();
        return ResponseEntity.ok(nodes);
    }
}
