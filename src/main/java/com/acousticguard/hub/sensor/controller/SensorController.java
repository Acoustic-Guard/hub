package com.acousticguard.hub.sensor.controller;

import com.acousticguard.hub.common.enums.SensorStatus;
import com.acousticguard.hub.sensor.model.Sensor;
import com.acousticguard.hub.sensor.service.SensorMonitorService;
import com.acousticguard.hub.telemetry.service.TelemetryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for sensor management.
 * Provides endpoints for sensor telemetry and status.
 */
@RestController
@RequestMapping("/api/v1/telemetry")
@RequiredArgsConstructor
public class SensorController {

    private final SensorMonitorService sensorMonitorService;
    private final TelemetryService telemetryService;

    /**
     * Gets all sensor nodes with their status.
     * 
     * @return list of sensors with status
     */
    @GetMapping("/nodes")
    public ResponseEntity<List<Map<String, Object>>> getAllNodes() {
        List<Sensor> sensors = sensorMonitorService.getAllSensors();
        
        List<Map<String, Object>> nodes = sensors.stream()
                .map(sensor -> {
                    Map<String, Object> nodeMap = new java.util.HashMap<>();
                    nodeMap.put("id", sensor.getId());
                    nodeMap.put("location", sensor.getLocation());
                    nodeMap.put("latitude", sensor.getLatitude());
                    nodeMap.put("longitude", sensor.getLongitude());
                    nodeMap.put("status", sensor.getStatus());
                    nodeMap.put("lastHeartbeat", sensor.getLastHeartbeat() != null ? sensor.getLastHeartbeat().toString() : "N/A");
                    return nodeMap;
                })
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(nodes);
    }
}
