package com.acousticguard.hub.common.error;

/**
 * Base sealed interface for domain errors.
 * All business logic errors should extend this interface.
 */
public sealed interface DomainError permits SensorNotFoundError, ConfidenceThresholdNotMetError, IncidentNotFoundError, AlertNotFoundError {

    /**
     * Returns the error message describing the domain error.
     * 
     * @return the error message
     */
    String getMessage();
}
