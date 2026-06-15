package com.acousticguard.hub.telemetry.service.impl;

import com.acousticguard.hub.common.enums.SensorStatus;
import com.acousticguard.hub.common.enums.SystemStatus;
import com.acousticguard.hub.monitoring.MessageLoadMonitor;
import com.acousticguard.hub.telemetry.dto.SensorNodeResponseDto;
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
 * <p>
 * Stored entirely in memory to prevent database bottlenecks during high-frequency
 * telemetry tracking. This record encapsulates the current noise level, GPS location,
 * network latency, and last seen timestamp for a single sensor node.
 * </p>
 *
 * @param avgDb     the processed decibel level (dB SPL) for visualization
 * @param latitude  GPS latitude of the sensor
 * @param longitude GPS longitude of the sensor
 * @param latencyMs network transmission latency between edge agent capture and hub processing
 * @param lastSeen  the exact UTC timestamp when the node last reported its state
 */
record NodeState(float avgDb, float latitude, float longitude, long latencyMs, Instant lastSeen) {
}

/**
 * Core business logic implementation for managing acoustic sensor telemetry.
 * <p>
 * This service acts as an orchestrator for real-time telemetry processing. It utilizes
 * a highly concurrent in-memory cache (ConcurrentHashMap) for real-time state retrieval
 * and delegates database persistence to an external adapter to strictly adhere to the
 * Single Responsibility Principle (SRP). The service runs two background jobs:
 * a sensor reaper to evict dead nodes, and a telemetry broadcaster to push updates
 * to WebSocket clients.
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

    // Message load monitor for tracking events per minute
    private final MessageLoadMonitor messageLoadMonitor;

    @Value("${acoustic.sensor.heartbeat-timeout-seconds:60}")
    private long heartbeatTimeoutSeconds;

    /**
     * Processes incoming telemetry events, updates the in-memory state map,
     * calculates network latency, and delegates persistence operations.
     * <p>
     * This method converts raw dBFS values to dB SPL for frontend display,
     * calculates one-way network latency, and updates the thread-safe node state cache.
     * If a sensor was previously offline, it publishes a recovery event via WebSocket.
     * Database persistence is delegated to a throttled adapter to prevent write amplification.
     * </p>
     *
     * @param event the structured telemetry payload received from the message broker,
     *              containing sensor ID, location, noise level, and capture timestamp
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

        // Check if sensor was previously offline (not in the map)
        boolean wasOffline = !nodeStates.containsKey(event.sensorId());

        // Update the thread-safe cache
        nodeStates.put(event.sensorId(), state);

        log.debug("Telemetry updated for sensor {}: {} dB (display), {} ms latency",
                event.sensorId(), displayDb, latencyMs);

        // If sensor was previously offline, publish ONLINE status update immediately
        if (wasOffline) {
            log.info("Sensor {} recovered from offline. Publishing ONLINE status update.", event.sensorId());
            try {
                // Publish sensor recovery event to /topic/sensors
                SensorNodeResponseDto recoveryEvent = new SensorNodeResponseDto(
                        event.sensorId(),
                        null, // location - not available from telemetry
                        SensorStatus.ONLINE,
                        (int) latencyMs,
                        null, // uptimePercent - not available from telemetry
                        Instant.now(),
                        event.latitude(),
                        event.longitude(),
                        null, // firmwareVersion - not available from telemetry
                        null  // metadata - not available from telemetry
                );
                simpMessagingTemplate.convertAndSend("/topic/sensors", recoveryEvent);
            } catch (Exception e) {
                log.error("Failed to publish sensor recovery event for sensor {}", event.sensorId(), e);
            }
        }

        // Delegate persistence to the adapter.
        // This call crosses the class boundary, triggering the @Transactional proxy correctly.
        sensorPersistenceService.persistNoiseDataWithThrottle(
                event.sensorId(), displayDb, event.latitude(), event.longitude()
        );
    }

    /**
     * Aggregates the current real-time state of the entire sensor network.
     * <p>
     * This method calculates averages on the fly from the in-memory cache,
     * ensuring extremely fast response times for the frontend dashboard.
     * It computes average noise levels, latency, and evaluates system health
     * status based on aggregated metrics. All calculations are performed
     * in-memory to avoid database queries.
     * </p>
     *
     * @return a comprehensive DTO containing system-wide metrics including
     * active node count, average latency, noise level, and system status indicators
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
                Instant.now(),
                messageLoadMonitor.getLastMinuteEventCount()
        );
    }

    /**
     * Resolves the current operational status of a specific sensor.
     * <p>
     * This method evaluates the in-memory state to determine if the heartbeat
     * is within the acceptable threshold. A sensor is considered ONLINE if it
     * has transmitted telemetry within the configured heartbeat timeout period.
     * </p>
     *
     * @param sensorId the unique identifier of the sensor to query
     * @return ONLINE if the sensor has transmitted within the heartbeat timeout,
     * OFFLINE otherwise
     */
    @Override
    public SensorStatus getSensorStatus(String sensorId) {
        NodeState state = nodeStates.get(sensorId);
        if (state == null) {
            return SensorStatus.OFFLINE;
        }

        Instant threshold = Instant.now().minusSeconds(heartbeatTimeoutSeconds);
        return state.lastSeen().isAfter(threshold) ? SensorStatus.ONLINE : SensorStatus.OFFLINE;
    }

    /**
     * Gets the current network latency for a specific sensor.
     * <p>
     * This method retrieves the network latency from the in-memory node state cache.
     * Returns null if the sensor is offline or has not transmitted telemetry recently.
     * </p>
     *
     * @param sensorId the unique identifier of the sensor to query
     * @return the network latency in milliseconds, or null if the sensor is offline
     * or not found in the telemetry cache
     */
    @Override
    public Long getSensorLatency(String sensorId) {
        NodeState state = nodeStates.get(sensorId);
        if (state == null) {
            return null;
        }
        return state.latencyMs();
    }

    /**
     * Reaper job executed continuously in the background.
     * <p>
     * Scans the active node map and evicts any sensors that have stopped transmitting telemetry.
     * Operates purely on the functional map entry level to ensure thread safety
     * and prevent ConcurrentModificationException. Sensors that have not transmitted
     * within the heartbeat timeout period are removed from the cache.
     * </p>
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
     * <p>
     * Executes every 2 seconds to ensure the frontend dashboard displays current system state.
     * Uses the same STOMP topic (/topic/telemetry) that the React frontend subscribes to.
     * This method aggregates system metrics and pushes them to all connected clients.
     * </p>
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