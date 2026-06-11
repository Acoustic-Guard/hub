package com.acousticguard.hub.telemetry.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for telemetry events from RabbitMQ q.telemetry queue.
 * Contains low-frequency data strictly for the noise map.
 * Uses camelCase JSON property names to match Rust serialization.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TelemetryEvent(
        @JsonProperty("sensorId")
        String sensorId,

        @JsonProperty("capturedAtMs")
        long capturedAtMs,

        @JsonProperty("latitude")
        float latitude,

        @JsonProperty("longitude")
        float longitude,

        @JsonProperty("avgDb")
        float avgDb
) {
}
