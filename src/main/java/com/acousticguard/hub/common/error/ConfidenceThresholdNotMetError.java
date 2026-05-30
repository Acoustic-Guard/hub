package com.acousticguard.hub.common.error;

/**
 * Domain error thrown when classification confidence is below the required threshold.
 */
public class ConfidenceThresholdNotMetError extends DomainError {

    public ConfidenceThresholdNotMetError(float actualConfidence, float requiredThreshold) {
        super(String.format("Confidence %.2f is below required threshold %.2f", actualConfidence, requiredThreshold));
    }
}
