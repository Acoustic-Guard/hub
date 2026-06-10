package com.acousticguard.hub.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record NoisePointDto(
        Float latitude,
        Float longitude,
        Float db
) {
}
