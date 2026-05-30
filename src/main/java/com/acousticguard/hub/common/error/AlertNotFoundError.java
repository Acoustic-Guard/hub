package com.acousticguard.hub.common.error;

import java.util.UUID;

/**
 * Domain error thrown when an alert is not found.
 */
public class AlertNotFoundError extends DomainError {

    public AlertNotFoundError(UUID alertId) {
        super("Alert not found with id: " + alertId);
    }
}
