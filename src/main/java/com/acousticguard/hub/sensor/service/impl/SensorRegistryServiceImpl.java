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
 * <p>
 * Handles CRUD operations and sensor state management. This implementation
 * provides read-only access to the sensor registry, delegating database
 * queries to the SensorRepository. All operations are performed within
 * read-only transactions to optimize performance and prevent accidental
 * data modification.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class SensorRegistryServiceImpl implements SensorRegistryService {

    private final SensorRepository sensorRepository;

    /**
     * Retrieves all sensors from the database.
     * <p>
     * This method performs a read-only transaction to fetch all registered sensors.
     * The operation is optimized for performance by using a read-only transaction,
     * which allows the database to avoid acquiring write locks and enables
     * potential query optimization.
     * </p>
     *
     * @return a list of all sensors in the system, or an empty list if no sensors exist
     */
    @Override
    @Transactional(readOnly = true)
    public List<Sensor> getAllSensors() {
        return sensorRepository.findAll();
    }
}
