package com.acousticguard.hub.sensor.service.impl;

import com.acousticguard.hub.common.enums.SensorStatus;
import com.acousticguard.hub.sensor.model.Sensor;
import com.acousticguard.hub.sensor.repository.SensorRepository;
import com.acousticguard.hub.sensor.service.SensorMonitorService;
import com.acousticguard.hub.telemetry.service.TelemetryService;
import com.acousticguard.hub.websocket.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of SensorMonitorService.
 * Handles sensor health monitoring using in-memory telemetry data.
 * Calculates sensor status on the fly to avoid DB bottlenecks.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SensorMonitorServiceImpl implements SensorMonitorService {

    private final SensorRepository sensorRepository;
    private final TelemetryService telemetryService;
    private final com.acousticguard.hub.websocket.EventPublisherPort eventPublisherPort;

    @Value("${acoustic.sensor.heartbeat-timeout-seconds:10}")
    private long heartbeatTimeoutSeconds;

    @Override
    public void updateHeartbeat(String sensorId) {
        // Heartbeat is now tracked in-memory via TelemetryService
        // No DB write needed
        log.debug("Heartbeat tracked in-memory for sensor {}", sensorId);
    }

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
                eventPublisherPort.publishSensorStatus(sensor);
            } else if (currentStatus == SensorStatus.ONLINE && sensor.getStatus() != SensorStatus.ONLINE) {
                sensor.setStatus(SensorStatus.ONLINE);
                sensorRepository.save(sensor);
                log.info("Sensor {} marked as online", sensor.getId());
                eventPublisherPort.publishSensorStatus(sensor);
            }
        }
        
        return offlineSensors;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Sensor> getAllSensors() {
        return sensorRepository.findAll();
    }
}
