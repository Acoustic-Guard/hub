package com.acousticguard.hub.common.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum IncidentStatus {
    DETECTED("Detected"),
    INVESTIGATING("Investigating"),
    CONFIRMED("Confirmed"),
    RESOLVED("Resolved");

    private final String value;

    IncidentStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}