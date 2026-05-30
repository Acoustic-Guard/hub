package com.acousticguard.hub.telemetry.service.impl;

import com.acousticguard.hub.common.enums.SystemStatus;
import com.acousticguard.hub.sensor.dto.AudioFrame;
import com.acousticguard.hub.sensor.repository.SensorRepository;
import com.acousticguard.hub.telemetry.dto.TelemetryResponseDto;
import com.acousticguard.hub.telemetry.service.TelemetryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Internal record to store node state including latency, coordinates, and noise level.
 */
record NodeState(float avgDb, float latency, float latitude, float longitude, Instant lastSeen) {}

/**
 * Implementation of TelemetryService.
 * Handles telemetry collection and reporting using abstractions for data access.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TelemetryServiceImpl implements TelemetryService {

    private final SensorRepository sensorRepository;
    
    private final Map<String, NodeState> nodeStates = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public void updateNodeTelemetry(AudioFrame frame) {
        // Calculate latency as time difference between capture and now
        long latencyMs = Instant.now().toEpochMilli() - frame.capturedAtMs();
        
        NodeState state = new NodeState(
                frame.avgDb(),
                latencyMs,
                frame.latitude(),
                frame.longitude(),
                Instant.now()
        );
        
        nodeStates.put(frame.sensorId(), state);
        
        log.debug("Telemetry updated for sensor {}: {} dB, {}ms latency, {}, {}", 
                frame.sensorId(), frame.avgDb(), latencyMs, frame.latitude(), frame.longitude());
    }

    @Override
    @Transactional(readOnly = true)
    public TelemetryResponseDto getSystemTelemetry() {
        int activeNodes = (int) sensorRepository.count();
        
        double avgSystemNoise = nodeStates.values().stream()
                .mapToDouble(NodeState::avgDb)
                .average()
                .orElse(0.0);

        double avgLatency = nodeStates.values().stream()
                .mapToDouble(NodeState::latency)
                .average()
                .orElse(0.0);

        SystemStatus noiseStatus = avgSystemNoise > 70.0 ? SystemStatus.WARNING : SystemStatus.NORMAL;
        SystemStatus nodesStatus = activeNodes > 0 ? SystemStatus.NORMAL : SystemStatus.CRITICAL;

        // Calculate offline nodes
        Instant threshold = Instant.now().minusSeconds(10);
        long offlineNodes = sensorRepository.findByLastHeartbeatBefore(threshold).size();

        return new TelemetryResponseDto(
                activeNodes,
                (int) Math.round(avgLatency),
                (int) Math.round(avgSystemNoise),
                nodesStatus,
                SystemStatus.NORMAL,
                noiseStatus,
                (int) offlineNodes,
                0, 
                99.9f, 
                Instant.now()
        );
    }
}
