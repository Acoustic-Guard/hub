package com.acousticguard.hub.sensor.service.impl;

import com.acousticguard.hub.common.enums.SensorStatus;
import com.acousticguard.hub.port.EventPublisherPort;
import com.acousticguard.hub.sensor.model.Sensor;
import com.acousticguard.hub.sensor.repository.SensorRepository;
import com.acousticguard.hub.sensor.service.SensorMonitorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Implementation of SensorMonitorService.
 * Handles sensor health monitoring with heartbeat timeout detection.
 * Marks sensors as offline if no heartbeat received within 10 seconds.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SensorMonitorServiceImpl implements SensorMonitorService {

    private final SensorRepository sensorRepository;
    private final EventPublisherPort eventPublisherPort;

    @Value("${acoustic.sensor.heartbeat-timeout-seconds:10}")
    private long heartbeatTimeoutSeconds;

    @Override
    @Transactional
    public void updateHeartbeat(String sensorId) {
        Sensor sensor = sensorRepository.findById(sensorId)
                .orElseGet(() -> createSensor(sensorId));
        
        sensor.setLastHeartbeat(Instant.now());
        sensor.setStatus(SensorStatus.ONLINE);
        sensorRepository.save(sensor);
        
        log.debug("Heartbeat updated for sensor {}", sensorId);
    }

    @Override
    @Scheduled(fixedRate = 5000)
    @Transactional
    public List<Sensor> checkOfflineSensors() {
        Instant threshold = Instant.now().minusSeconds(heartbeatTimeoutSeconds);
        List<Sensor> offlineSensors = sensorRepository.findByLastHeartbeatBefore(threshold);
        
        offlineSensors.forEach(sensor -> {
            if (sensor.getStatus() != SensorStatus.OFFLINE) {
                sensor.setStatus(SensorStatus.OFFLINE);
                sensorRepository.save(sensor);
                log.warn("Sensor {} marked as offline (last heartbeat: {})", 
                        sensor.getId(), sensor.getLastHeartbeat());
                
                eventPublisherPort.publish(sensor);
            }
        });
        
        return offlineSensors;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Sensor> getAllSensors() {
        return sensorRepository.findAll();
    }

    private Sensor createSensor(String sensorId) {
        return Sensor.builder()
                .id(sensorId)
                .location("Unknown")
                .latitude(0.0f)
                .longitude(0.0f)
                .status(SensorStatus.OFFLINE)
                .lastHeartbeat(Instant.now())
                .build();
    }
}
