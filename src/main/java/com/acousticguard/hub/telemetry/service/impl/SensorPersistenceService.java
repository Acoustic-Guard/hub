package com.acousticguard.hub.telemetry.service.impl;

import com.acousticguard.hub.common.enums.SensorStatus;
import com.acousticguard.hub.sensor.model.Sensor;
import com.acousticguard.hub.sensor.repository.SensorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * Persistence adapter for sensor telemetry.
 * Handles database operations with proper Spring AOP transaction management.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SensorPersistenceService {

    private final SensorRepository sensorRepository;

    /**
     * Persists noise data to the database with a 5-minute throttling strategy.
     * Auto-registers new sensors upon their first telemetry ping.
     *
     * @param sensorId  Unique identifier of the edge node.
     * @param avgDb     Processed decibel level (dB SPL).
     * @param latitude  GPS latitude.
     * @param longitude GPS longitude.
     */
    @Transactional
    public void persistNoiseDataWithThrottle(String sensorId, float avgDb, float latitude, float longitude) {
        try {
            Sensor sensor = sensorRepository.findById(sensorId).orElse(null);
            Instant now = Instant.now();

            if (sensor == null) {
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
                Instant threshold = now.minus(Duration.ofMinutes(5));

                if (sensor.getNoiseUpdatedAt() == null || sensor.getNoiseUpdatedAt().isBefore(threshold)) {
                    sensor.setStatus(SensorStatus.ONLINE);
                    sensor.setLastHeartbeat(now);
                    sensor.setCurrentAvgDb(avgDb);
                    sensor.setNoiseUpdatedAt(now);
                    sensorRepository.save(sensor);
                    log.debug("Persisted throttled noise data for sensor {}: {} dB", sensorId, avgDb);
                }
            }
        } catch (Exception e) {
            log.error("Failed to persist noise data for sensor {}", sensorId, e);
        }
    }
}