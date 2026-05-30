package com.acousticguard.hub.common.error;

/**
 * Domain error thrown when a sensor is not found.
 */
public class SensorNotFoundError extends DomainError {

    public SensorNotFoundError(String sensorId) {
        super("Sensor not found with id: " + sensorId);
    }
}
