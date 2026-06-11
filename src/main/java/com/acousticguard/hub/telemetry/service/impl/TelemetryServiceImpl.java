package com.acousticguard.hub.telemetry.service.impl;

import com.acousticguard.hub.common.enums.SensorStatus;
import com.acousticguard.hub.common.enums.SystemStatus;
import com.acousticguard.hub.sensor.model.Sensor;
import com.acousticguard.hub.sensor.repository.SensorRepository;
import com.acousticguard.hub.telemetry.dto.TelemetryEvent;
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
 * Internal record to store node state including noise level, coordinates, and last seen timestamp.
 */
record NodeState(float avgDb, float latitude, float longitude, Instant lastSeen) {
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
    public void updateNodeTelemetry(TelemetryEvent event) {
        NodeState existingState = nodeStates.get(event.sensorId());

        // Convert dBFS (negative, e.g., -60.0 to 0.0) to dB SPL (positive, e.g., 40.0 to 90.0)
        // Formula: displayDb = Math.max(30.0f, event.avgDb() + 100.0f)
        // This shifts -60 dBFS to 40 dB SPL, and clamps minimum to 30 dB for ambient silence
        float rawDbFs = event.avgDb();
        float displayDb = Math.max(30.0f, rawDbFs + 100.0f);

        NodeState state = new NodeState(
                displayDb,
                event.latitude(),
                event.longitude(),
                Instant.now()
        );

        nodeStates.put(event.sensorId(), state);

        log.debug("Telemetry updated for sensor {}: {} dB (display), {} dBFS (raw), lat={}, lng={}",
                event.sensorId(), displayDb, rawDbFs, event.latitude(), event.longitude());

        // Throttled DB persistence for noise data (always call to ensure auto-registration)
        persistNoiseDataWithThrottle(event.sensorId(), displayDb, event.latitude(), event.longitude());
    }

    /**
     * Persists noise data to the database with a 5-minute throttle.
     * Updates the sensor's current_avg_db and noise_updated_at only if:
     * - noise_updated_at is null (first update), OR
     * - more than 5 minutes have passed since the last update
     *
     * If the sensor does not exist in the database, it will be auto-registered
     * with the provided telemetry data.
     *
     * @param sensorId the sensor identifier
     * @param avgDb    the average decibel level (display value in dB SPL)
     * @param latitude the sensor latitude
     * @param longitude the sensor longitude
     */
    @Transactional
    public void persistNoiseDataWithThrottle(String sensorId, float avgDb, float latitude, float longitude) {
        try {
            Sensor sensor = sensorRepository.findById(sensorId).orElse(null);
            Instant now = Instant.now();

            if (sensor == null) {
                // Auto-register new sensor
                sensor = Sensor.builder()
                        .id(sensorId)
                        .location("Auto-registered")
                        .latitude(latitude)
                        .longitude(longitude)
                        .status(SensorStatus.ONLINE)
                        .currentAvgDb(avgDb)
                        .lastHeartbeat(now)
                        .noiseUpdatedAt(now)
                        .build();
                sensorRepository.save(sensor);
                log.info("Auto-registered new sensor {}: lat={}, lng={}, avgDb={} dB",
                        sensorId, latitude, longitude, avgDb);
            } else {
                // Update existing sensor - only save if 5-minute threshold has passed
                Instant threshold = now.minus(Duration.ofMinutes(5));

                // Check if we should update noise data (null or older than 5 minutes)
                if (sensor.getNoiseUpdatedAt() == null || sensor.getNoiseUpdatedAt().isBefore(threshold)) {
                    sensor.setStatus(SensorStatus.ONLINE);
                    sensor.setLastHeartbeat(now);
                    sensor.setCurrentAvgDb(avgDb);
                    sensor.setNoiseUpdatedAt(now);
                    sensorRepository.save(sensor);
                    log.debug("Persisted noise data for sensor {}: {} dB", sensorId, avgDb);
                } else {
                    log.debug("Skipping DB update for sensor {} (throttled, last update {})",
                            sensorId, sensor.getNoiseUpdatedAt());
                }
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

        SystemStatus noiseStatus = avgSystemNoise > 70.0 ? SystemStatus.WARNING : SystemStatus.NORMAL;
        SystemStatus nodesStatus = activeNodes > 0 ? SystemStatus.NORMAL : SystemStatus.CRITICAL;

        // Calculate offline nodes from in-memory data
        Instant threshold = Instant.now().minusSeconds(10);
        long offlineNodes = nodeStates.values().stream()
                .filter(state -> state.lastSeen().isBefore(threshold))
                .count();

        return new TelemetryResponseDto(
                activeNodes,
                0, // Latency not applicable for telemetry-only flow
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
