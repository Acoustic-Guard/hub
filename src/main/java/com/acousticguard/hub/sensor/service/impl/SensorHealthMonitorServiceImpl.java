package com.acousticguard.hub.sensor.service.impl;

import com.acousticguard.hub.common.enums.SensorStatus;
import com.acousticguard.hub.sensor.mapper.SensorMapper;
import com.acousticguard.hub.sensor.model.Sensor;
import com.acousticguard.hub.sensor.repository.SensorRepository;
import com.acousticguard.hub.sensor.service.SensorHealthMonitorService;
import com.acousticguard.hub.telemetry.service.TelemetryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of SensorHealthMonitorService.
 * <p>
 * Handles heartbeat tracking and scheduled offline sensor checking. This implementation
 * runs a background job every 5 seconds to scan all registered sensors and detect
 * connectivity issues. Sensors that have not transmitted telemetry within the
 * configured timeout period are marked as OFFLINE in the database, and status change
 * events are published via WebSocket for real-time frontend updates.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SensorHealthMonitorServiceImpl implements SensorHealthMonitorService {

    private final SensorRepository sensorRepository;
    private final TelemetryService telemetryService;
    private final com.acousticguard.hub.websocket.EventPublisherPort eventPublisherPort;
    private final SensorMapper sensorMapper;

    @Value("${acoustic.sensor.heartbeat-timeout-seconds:60}")
    private long heartbeatTimeoutSeconds;

    /**
     * Updates the heartbeat timestamp for a sensor.
     * <p>
     * This method is called when a heartbeat is received from a sensor. The heartbeat
     * is tracked in-memory via the TelemetryService to avoid unnecessary database writes.
     * This design choice reduces database load during high-frequency telemetry processing.
     * </p>
     *
     * @param sensorId the unique identifier of the sensor that sent the heartbeat
     */
    @Override
    public void updateHeartbeat(String sensorId) {
        // Heartbeat is now tracked in-memory via TelemetryService
        // No DB write needed
        log.debug("Heartbeat tracked in-memory for sensor {}", sensorId);
    }

    /**
     * Checks all sensors and marks those without recent heartbeats as offline.
     * <p>
     * This method is executed every 5 seconds via Spring's @Scheduled annotation.
     * It queries all registered sensors from the database, checks their current status
     * against the in-memory telemetry cache, and performs status transitions when
     * necessary. Sensors that transition to OFFLINE are returned for monitoring purposes.
     * Status changes are persisted to the database and broadcast via WebSocket.
     * </p>
     *
     * @return a list of sensors that were newly marked as offline during this check,
     * or an empty list if no sensors transitioned to offline
     */
    @Override
    @Scheduled(fixedRate = 5000)
    @Transactional
    public List<Sensor> checkOfflineSensors() {
        List<Sensor> allSensors = sensorRepository.findAll();
        List<Sensor> offlineSensors = new java.util.ArrayList<>();

        for (Sensor sensor : allSensors) {
            SensorStatus currentStatus = telemetryService.getSensorStatus(sensor.getId());

            if (currentStatus == SensorStatus.OFFLINE && sensor.getStatus() != SensorStatus.OFFLINE) {
                sensor.setStatus(SensorStatus.OFFLINE);
                sensorRepository.save(sensor);
                offlineSensors.add(sensor);
                log.warn("Sensor {} marked as offline", sensor.getId());

                // Get DTO and update with latency (null for offline)
                var dto = sensorMapper.toDto(sensor);
                eventPublisherPort.publishSensorStatus(dto);
            } else if (currentStatus == SensorStatus.ONLINE && sensor.getStatus() != SensorStatus.ONLINE) {
                sensor.setStatus(SensorStatus.ONLINE);
                sensorRepository.save(sensor);
                log.info("Sensor {} marked as online", sensor.getId());

                // Get DTO and update with latency from telemetry
                var dto = sensorMapper.toDto(sensor);
                Long latency = telemetryService.getSensorLatency(sensor.getId());
                if (dto.latencyMs() == null && latency != null) {
                    // Create new DTO with latency (since records are immutable)
                    dto = new com.acousticguard.hub.telemetry.dto.SensorNodeResponseDto(
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
                eventPublisherPort.publishSensorStatus(dto);
            }
        }

        return offlineSensors;
    }
}
