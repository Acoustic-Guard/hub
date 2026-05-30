package com.acousticguard.hub.common.error;

/**
 * Base class for domain errors.
 * All business logic errors should extend this class.
 */
public abstract class DomainError extends RuntimeException {

    public DomainError(String message) {
        super(message);
    }
}
