package com.acousticguard.hub.sensor.service;

import com.acousticguard.hub.sensor.model.Sensor;

import java.util.List;

/**
 * Service interface for sensor registry operations.
 * Handles CRUD operations and sensor state management.
 */
public interface SensorRegistryService {

    /**
     * Retrieves all sensors with their current status.
     *
     * @return list of all sensors
     */
    List<Sensor> getAllSensors();
}
