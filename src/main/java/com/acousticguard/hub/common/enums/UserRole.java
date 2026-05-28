package com.acousticguard.hub.common.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum UserRole {
    GUEST("guest"),
    ADMIN("admin");

    private final String value;

    UserRole(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}