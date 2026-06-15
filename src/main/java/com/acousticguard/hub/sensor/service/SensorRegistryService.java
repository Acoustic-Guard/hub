package com.acousticguard.hub.sensor.service;

import com.acousticguard.hub.sensor.model.Sensor;

import java.util.List;

/**
 * Service interface for sensor registry operations.
 * <p>
 * Handles CRUD operations and sensor state management. This service is responsible
 * for querying the sensor registry and providing sensor data to controllers and other
 * services. It focuses on data retrieval and persistence operations, separating these
 * concerns from health monitoring and heartbeat tracking.
 * </p>
 */
public interface SensorRegistryService {

    /**
     * Retrieves all sensors with their current status from the database.
     * <p>
     * This method performs a read-only transaction to fetch all registered sensors
     * regardless of their status (ONLINE, OFFLINE, etc.). The returned list includes
     * all sensor metadata including location, firmware version, and current status.
     * </p>
     *
     * @return a list of all sensors in the system, or an empty list if no sensors exist
     */
    List<Sensor> getAllSensors();
}
