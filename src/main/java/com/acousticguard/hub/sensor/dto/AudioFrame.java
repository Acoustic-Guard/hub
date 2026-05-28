package com.acousticguard.hub.sensor.dto;

import java.util.List;

public record AudioFrame(
    String sensorId,
    long capturedAtMs,
    float latitude,
    float longitude,
    List<Float> fftBins,
    float sampleRateHz,
    Float peakDb
) {}