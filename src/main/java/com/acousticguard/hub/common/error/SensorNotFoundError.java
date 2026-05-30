package com.acousticguard.hub.common.error;

/**
 * Domain error thrown when a sensor is not found.
 */
public record SensorNotFoundError(String sensorId) implements DomainError {

    @Override
    public String getMessage() {
        return "Sensor not found with id: " + sensorId;
    }
}
