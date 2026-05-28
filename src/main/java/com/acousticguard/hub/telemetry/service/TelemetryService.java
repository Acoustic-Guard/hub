package com.acousticguard.hub.telemetry.service;

import com.acousticguard.hub.common.enums.SystemStatus;
import com.acousticguard.hub.telemetry.dto.TelemetryResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class TelemetryService {

    private final Map<String, Float> nodeNoiseLevels = new ConcurrentHashMap<>();
    
    private final Map<String, Instant> nodeLastSeen = new ConcurrentHashMap<>();

    public void updateNodeNoiseLevel(String sensorId, Float avgDb) {
        nodeNoiseLevels.put(sensorId, avgDb);
        nodeLastSeen.put(sensorId, Instant.now());
        log.debug("Telemetry updated for sensor {}: {} dB", sensorId, avgDb);
    }

    public TelemetryResponseDto getSystemTelemetry() {
        int activeNodes = nodeNoiseLevels.size();
        
        double avgSystemNoise = nodeNoiseLevels.values().stream()
                .mapToDouble(Float::doubleValue)
                .average()
                .orElse(0.0);

        SystemStatus noiseStatus = avgSystemNoise > 70.0 ? SystemStatus.WARNING : SystemStatus.NORMAL;
        SystemStatus nodesStatus = activeNodes > 0 ? SystemStatus.NORMAL : SystemStatus.CRITICAL;

        return new TelemetryResponseDto(
                activeNodes,
                15, // TODO: Розрахунок latency з пінгів RabbitMQ
                (int) Math.round(avgSystemNoise),
                nodesStatus,
                SystemStatus.NORMAL,
                noiseStatus,
                0, // TODO: Логіка підрахунку offline нод (через nodeLastSeen)
                0, 
                99.9f, 
                Instant.now()
        );
    }
}