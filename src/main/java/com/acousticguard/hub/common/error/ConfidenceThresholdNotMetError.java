package com.acousticguard.hub.common.error;

/**
 * Domain error thrown when classification confidence is below the required threshold.
 */
public record ConfidenceThresholdNotMetError(float actualConfidence, float requiredThreshold) implements DomainError {

    @Override
    public String getMessage() {
        return String.format("Confidence %.2f is below required threshold %.2f", actualConfidence, requiredThreshold);
    }
}
