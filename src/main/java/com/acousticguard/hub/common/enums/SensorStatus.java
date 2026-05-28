package com.acousticguard.hub.common.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum SensorStatus {
    ONLINE("online"),
    OFFLINE("offline"),
    WARNING("warning");

    private final String value;

    SensorStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}