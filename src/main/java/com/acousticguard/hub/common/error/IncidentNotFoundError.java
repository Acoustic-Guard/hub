package com.acousticguard.hub.common.error;

import java.util.UUID;

/**
 * Domain error thrown when an incident is not found.
 */
public record IncidentNotFoundError(UUID incidentId) implements DomainError {

    @Override
    public String getMessage() {
        return "Incident not found with id: " + incidentId;
    }
}
