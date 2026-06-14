package com.acousticguard.hub.telemetry.service.impl;

import com.acousticguard.hub.common.enums.SensorStatus;
import com.acousticguard.hub.common.enums.SystemStatus;
import com.acousticguard.hub.telemetry.dto.TelemetryEvent;
import com.acousticguard.hub.telemetry.dto.TelemetryResponseDto;
import com.acousticguard.hub.telemetry.service.TelemetryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Immutable record representing the real-time state of an edge node.
 * Stored entirely in memory to prevent database bottlenecks during high-frequency telemetry tracking.
 *
 * @param avgDb     The processed decibel level (dB SPL) for visualization.
 * @param latitude  GPS latitude of the sensor.
 * @param longitude GPS longitude of the sensor.
 * @param latencyMs Network transmission latency between the edge agent capture and hub processing.
 * @param lastSeen  The exact UTC timestamp when the node last reported its state.
 */
record NodeState(float avgDb, float latitude, float longitude, long latencyMs, Instant lastSeen) {
}

/**
 * Core business logic implementation for managing acoustic sensor telemetry.
 * <p>
 * This service acts as an orchestrator. It utilizes a highly concurrent in-memory cache
 * for real-time state retrieval and delegates database persistence to an external
 * adapter to strictly adhere to the Single Responsibility Principle (SRP).
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TelemetryServiceImpl implements TelemetryService {

    // Depending on the abstraction (Interface), not the concretion.
    private final SensorPersistenceService sensorPersistenceService;

    // WebSocket messaging template for real-time telemetry broadcasts
    private final SimpMessagingTemplate simpMessagingTemplate;

    // Concurrent map to safely store and update node states from multiple async RabbitMQ listener threads
    private final Map<String, NodeState> nodeStates = new ConcurrentHashMap<>();

    @Value("${acoustic.sensor.heartbeat-timeout-seconds:10}")
    private long heartbeatTimeoutSeconds;

    /**
     * Processes incoming telemetry events, updates the in-memory state map,
     * calculates network latency, and delegates persistence operations.
     *
     * @param event The structured telemetry payload received from the message broker.
     */
    @Override
    public void updateNodeTelemetry(TelemetryEvent event) {
        long currentMs = System.currentTimeMillis();

        // Calculate one-way latency. Math.max prevents negative values in case of slight clock skews
        long latencyMs = Math.max(0, currentMs - event.capturedAtMs());

        // Adapter Pattern logic: Convert raw dBFS (negative) to dB SPL (positive) for frontend display.
        // Clamping the minimum to 30 dB to realistically represent ambient background silence.
        float rawDbFs = event.avgDb();
        float displayDb = Math.max(30.0f, rawDbFs + 100.0f);

        NodeState state = new NodeState(
                displayDb,
                event.latitude(),
                event.longitude(),
                latencyMs,
                Instant.now()
        );

        // Update the thread-safe cache
        nodeStates.put(event.sensorId(), state);

        log.debug("Telemetry updated for sensor {}: {} dB (display), {} ms latency",
                event.sensorId(), displayDb, latencyMs);

        // Delegate persistence to the adapter.
        // This call crosses the class boundary, triggering the @Transactional proxy correctly.
        sensorPersistenceService.persistNoiseDataWithThrottle(
                event.sensorId(), displayDb, event.latitude(), event.longitude()
        );
    }

    /**
     * Aggregates the current real-time state of the entire sensor network.
     * This method calculates averages on the fly from the in-memory cache,
     * ensuring extremely fast response times for the frontend dashboard.
     *
     * @return A comprehensive DTO containing system-wide metrics and health statuses.
     */
    @Override
    public TelemetryResponseDto getSystemTelemetry() {
        int activeNodes = nodeStates.size();

        double avgSystemNoise = nodeStates.values().stream()
                .mapToDouble(NodeState::avgDb)
                .average()
                .orElse(0.0);

        int avgLatency = (int) nodeStates.values().stream()
                .mapToLong(NodeState::latencyMs)
                .average()
                .orElse(0L);

        // Evaluate system health based on aggregated metrics
        SystemStatus noiseStatus = avgSystemNoise > 70.0 ? SystemStatus.WARNING : SystemStatus.NORMAL;
        SystemStatus nodesStatus = activeNodes > 0 ? SystemStatus.NORMAL : SystemStatus.CRITICAL;

        return new TelemetryResponseDto(
                activeNodes,
                avgLatency,
                (int) Math.round(avgSystemNoise),
                nodesStatus,
                SystemStatus.NORMAL,
                noiseStatus,
                0, // Offline nodes count is inherently 0 because dead nodes are evicted from the map
                0,
                99.9f,
                Instant.now()
        );
    }

    /**
     * Resolves the current operational status of a specific sensor.
     * Evaluates the in-memory state to determine if the heartbeat is within the acceptable threshold.
     *
     * @param sensorId Unique identifier of the sensor.
     * @return ONLINE if active within the configured heartbeat timeout, OFFLINE otherwise.
     */
    public SensorStatus getSensorStatus(String sensorId) {
        NodeState state = nodeStates.get(sensorId);
        if (state == null) {
            return SensorStatus.OFFLINE;
        }

        Instant threshold = Instant.now().minusSeconds(heartbeatTimeoutSeconds);
        return state.lastSeen().isAfter(threshold) ? SensorStatus.ONLINE : SensorStatus.OFFLINE;
    }

    /**
     * Gets the current latency for a specific sensor.
     *
     * @param sensorId Unique identifier of the sensor.
     * @return Latency in milliseconds, or null if sensor is offline or not found.
     */
    public Long getSensorLatency(String sensorId) {
        NodeState state = nodeStates.get(sensorId);
        if (state == null) {
            return null;
        }
        return state.latencyMs();
    }

    /**
     * Reaper job executed continuously in the background.
     * Scans the active node map and evicts any sensors that have stopped transmitting telemetry.
     * Operates purely on the functional map entry level to ensure thread safety
     * and prevent ConcurrentModificationException.
     */
    @Scheduled(fixedRate = 5000)
    public void sweepDeadSensors() {
        // Defines the time-to-live (TTL) for a sensor heartbeat based on configured timeout
        Instant deadThreshold = Instant.now().minus(heartbeatTimeoutSeconds, ChronoUnit.SECONDS);

        nodeStates.entrySet().removeIf(entry -> {
            boolean isDead = entry.getValue().lastSeen().isBefore(deadThreshold);
            if (isDead) {
                log.warn("Sensor {} went offline. Evicted from the active telemetry cache.", entry.getKey());
            }
            return isDead; // Returning true removes the entry from the map
        });
    }

    /**
     * Broadcasts real-time telemetry updates to all connected WebSocket clients.
     * Executes every 2 seconds to ensure the frontend dashboard displays current system state.
     * Uses the same STOMP topic (/topic/telemetry) that the React frontend subscribes to.
     */
    @Scheduled(fixedRate = 2000)
    public void broadcastTelemetry() {
        try {
            TelemetryResponseDto telemetry = getSystemTelemetry();
            simpMessagingTemplate.convertAndSend("/topic/telemetry", telemetry);
            log.debug("Telemetry broadcasted: {} nodes, {} ms latency, {} dB noise",
                    telemetry.activeNodes(), telemetry.avgLatencyMs(), telemetry.noiseLevelDb());
        } catch (Exception e) {
            log.error("Failed to broadcast telemetry", e);
        }
    }
}