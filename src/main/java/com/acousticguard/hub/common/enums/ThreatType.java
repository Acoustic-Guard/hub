package com.acousticguard.hub.common.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ThreatType {
    UAV("UAV"),
    EXPLOSION("Explosion"),
    SIREN("Siren"),
    GENERATOR("Generator"),
    TRUCK("Truck"),
    BACKGROUND("Background");

    private final String value;

    ThreatType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}