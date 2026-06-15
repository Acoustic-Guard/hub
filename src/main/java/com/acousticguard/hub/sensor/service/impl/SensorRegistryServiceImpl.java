package com.acousticguard.hub.sensor.service.impl;

import com.acousticguard.hub.sensor.model.Sensor;
import com.acousticguard.hub.sensor.repository.SensorRepository;
import com.acousticguard.hub.sensor.service.SensorRegistryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of SensorRegistryService.
 * Handles CRUD operations and sensor state management.
 */
@Service
@RequiredArgsConstructor
public class SensorRegistryServiceImpl implements SensorRegistryService {

    private final SensorRepository sensorRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Sensor> getAllSensors() {
        return sensorRepository.findAll();
    }
}
