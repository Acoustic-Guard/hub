package com.acousticguard.hub.analytics.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ThreatDistributionDto(
        String name,
        Long value
) {
}
