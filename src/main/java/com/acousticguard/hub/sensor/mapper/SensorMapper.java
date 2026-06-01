package com.acousticguard.hub.sensor.mapper;

import com.acousticguard.hub.common.config.MapperConfig;
import com.acousticguard.hub.sensor.model.Sensor;
import com.acousticguard.hub.telemetry.dto.SensorNodeResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapperConfig.class)
public interface SensorMapper {

    @Mapping(target = "latencyMs", ignore = true)
    @Mapping(target = "uptimePercent", ignore = true)
    @Mapping(target = "firmwareVersion", ignore = true)
    @Mapping(target = "metadata", ignore = true)
    SensorNodeResponseDto toDto(Sensor entity);
}
