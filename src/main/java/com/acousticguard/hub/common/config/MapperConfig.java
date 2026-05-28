package com.acousticguard.hub.common.config;

import com.acousticguard.hub.common.mapper.EnumFallbackMapper;
import org.mapstruct.ReportingPolicy;

@org.mapstruct.MapperConfig(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = {EnumFallbackMapper.class}
)
public interface MapperConfig {
}