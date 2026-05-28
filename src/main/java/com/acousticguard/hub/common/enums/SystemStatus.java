package com.acousticguard.hub.common.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum SystemStatus {
    NORMAL("normal"),
    WARNING("warning"),
    CRITICAL("critical");

    private final String value;

    SystemStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}