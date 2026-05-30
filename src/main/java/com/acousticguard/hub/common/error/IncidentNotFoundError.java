package com.acousticguard.hub.common.error;

import java.util.UUID;

/**
 * Domain error thrown when an incident is not found.
 */
public class IncidentNotFoundError extends DomainError {

    public IncidentNotFoundError(UUID incidentId) {
        super("Incident not found with id: " + incidentId);
    }
}
