package com.acousticguard.hub.telemetry.service.impl;

import com.acousticguard.hub.common.enums.SensorStatus;
import com.acousticguard.hub.common.enums.SystemStatus;
import com.acousticguard.hub.sensor.dto.AudioFrame;
import com.acousticguard.hub.sensor.model.Sensor;
import com.acousticguard.hub.sensor.repository.SensorRepository;
import com.acousticguard.hub.telemetry.dto.TelemetryResponseDto;
import com.acousticguard.hub.telemetry.service.TelemetryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Internal record to store node state including latency, coordinates, and noise level.
 */
record NodeState(float avgDb, float latency, float latitude, float longitude, Instant lastSeen) {
}

/**
 * Implementation of TelemetryService.
 * Handles telemetry collection and reporting using in-memory tracking.
 * Calculates sensor status on the fly to avoid DB bottlenecks.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TelemetryServiceImpl implements TelemetryService {

    private final SensorRepository sensorRepository;

    private final Map<String, NodeState> nodeStates = new ConcurrentHashMap<>();

    @Override
    public void updateNodeTelemetry(AudioFrame frame) {
        long latencyMs = Instant.now().toEpochMilli() - frame.capturedAtMs();

        NodeState existingState = nodeStates.get(frame.sensorId());

        float avgDb = frame.avgDb() != null ? frame.avgDb() :
                (existingState != null ? existingState.avgDb() : 0.0f);

        NodeState state = new NodeState(
                avgDb,
                latencyMs,
                frame.latitude(),
                frame.longitude(),
                Instant.now()
        );

        nodeStates.put(frame.sensorId(), state);

        log.debug("Telemetry updated for sensor {}: {} dB, {}ms latency, {}, {}",
                frame.sensorId(), avgDb, latencyMs, frame.latitude(), frame.longitude());

        // Throttled DB persistence for noise data
        if (frame.avgDb() != null) {
            persistNoiseDataWithThrottle(frame.sensorId(), frame.avgDb());
        }
    }

    /**
     * Persists noise data to the database with a 5-minute throttle.
     * Updates the sensor's current_avg_db and noise_updated_at only if:
     * - noise_updated_at is null (first update), OR
     * - more than 5 minutes have passed since the last update
     *
     * @param sensorId the sensor identifier
     * @param avgDb the average decibel level
     */
    private void persistNoiseDataWithThrottle(String sensorId, float avgDb) {
        try {
            Sensor sensor = sensorRepository.findById(sensorId).orElse(null);
            if (sensor == null) {
                log.debug("Sensor {} not found in database, skipping noise persistence", sensorId);
                return;
            }

            Instant now = Instant.now();
            Instant threshold = now.minus(Duration.ofMinutes(5));

            // Check if we should update (null or older than 5 minutes)
            if (sensor.getNoiseUpdatedAt() == null || sensor.getNoiseUpdatedAt().isBefore(threshold)) {
                sensor.setCurrentAvgDb(avgDb);
                sensor.setNoiseUpdatedAt(now);
                sensorRepository.save(sensor);
                log.debug("Persisted noise data for sensor {}: {} dB", sensorId, avgDb);
            } else {
                log.debug("Skipping noise persistence for sensor {} (throttled, last update {})",
                        sensorId, sensor.getNoiseUpdatedAt());
            }
        } catch (Exception e) {
            log.error("Failed to persist noise data for sensor {}", sensorId, e);
        }
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

        // Calculate offline nodes from in-memory data
        Instant threshold = Instant.now().minusSeconds(10);
        long offlineNodes = nodeStates.values().stream()
                .filter(state -> state.lastSeen().isBefore(threshold))
                .count();

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

    /**
     * Gets the status of a sensor based on in-memory heartbeat data.
     *
     * @param sensorId the sensor identifier
     * @return the sensor status
     */
    public SensorStatus getSensorStatus(String sensorId) {
        NodeState state = nodeStates.get(sensorId);
        if (state == null) {
            return SensorStatus.OFFLINE;
        }

        Instant threshold = Instant.now().minusSeconds(10);
        return state.lastSeen().isAfter(threshold) ? SensorStatus.ONLINE : SensorStatus.OFFLINE;
    }
}
