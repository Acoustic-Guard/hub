package com.acousticguard.hub.telemetry.service.impl;

import com.acousticguard.hub.common.enums.SystemStatus;
import com.acousticguard.hub.sensor.model.Sensor;
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
 * Implementation of TelemetryService.
 * Handles telemetry collection and reporting using abstractions for data access.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TelemetryServiceImpl implements TelemetryService {

    private final SensorRepository sensorRepository;
    
    private final Map<String, Float> nodeNoiseLevels = new ConcurrentHashMap<>();
    
    private final Map<String, Instant> nodeLastSeen = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public void updateNodeNoiseLevel(String sensorId, Float avgDb) {
        nodeNoiseLevels.put(sensorId, avgDb);
        nodeLastSeen.put(sensorId, Instant.now());
        
        // Update sensor last heartbeat
        sensorRepository.findById(sensorId).ifPresent(sensor -> {
            sensor.setLastHeartbeat(Instant.now());
            sensorRepository.save(sensor);
        });
        
        log.debug("Telemetry updated for sensor {}: {} dB", sensorId, avgDb);
    }

    @Override
    @Transactional(readOnly = true)
    public TelemetryResponseDto getSystemTelemetry() {
        int activeNodes = (int) sensorRepository.count();
        
        double avgSystemNoise = nodeNoiseLevels.values().stream()
                .mapToDouble(Float::doubleValue)
                .average()
                .orElse(0.0);

        SystemStatus noiseStatus = avgSystemNoise > 70.0 ? SystemStatus.WARNING : SystemStatus.NORMAL;
        SystemStatus nodesStatus = activeNodes > 0 ? SystemStatus.NORMAL : SystemStatus.CRITICAL;

        // Calculate offline nodes
        Instant threshold = Instant.now().minusSeconds(10);
        long offlineNodes = sensorRepository.findByLastHeartbeatBefore(threshold).size();

        return new TelemetryResponseDto(
                activeNodes,
                15, // TODO: Розрахунок latency з пінгів RabbitMQ
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
