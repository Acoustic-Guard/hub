package com.acousticguard.hub.common.error;

import java.util.UUID;

/**
 * Domain error thrown when an alert is not found.
 */
public record AlertNotFoundError(UUID alertId) implements DomainError {

    @Override
    public String getMessage() {
        return "Alert not found with id: " + alertId;
    }
}
