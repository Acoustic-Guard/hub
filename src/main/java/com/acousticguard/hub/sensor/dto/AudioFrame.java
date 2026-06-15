package com.acousticguard.hub.sensor.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * DTO for audio frames from RabbitMQ q.frames queue.
 * Contains high-frequency data strictly for ML threat classification.
 * Uses camelCase JSON property names to match Rust serialization.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AudioFrame(
        @JsonProperty("sensorId")
        String sensorId,

        @JsonProperty("capturedAtMs")
        long capturedAtMs,

        @JsonProperty("latitude")
        float latitude,

        @JsonProperty("longitude")
        float longitude,

        @JsonProperty("fftBins")
        List<Float> fftBins,

        @JsonProperty("sampleRateHz")
        float sampleRateHz,

        @JsonProperty("peakDb")
        Float peakDb,

        @JsonProperty("avgDb")
        Float avgDb,

        @JsonProperty("rawAudio")
        List<Short> rawAudio
) {
}