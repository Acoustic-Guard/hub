package com.acousticguard.hub.classifier.dto;

import com.acousticguard.hub.common.enums.ThreatType;

public record ClassificationResult(
    ThreatType threatType,
    float confidence,
    String modelVer
) {}