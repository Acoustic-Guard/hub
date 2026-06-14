package com.acousticguard.hub.sensor.mapper;

import com.acousticguard.hub.common.config.MapperConfig;
import com.acousticguard.hub.sensor.model.Sensor;
import com.acousticguard.hub.telemetry.dto.SensorNodeResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapperConfig.class)
public interface SensorMapper {

    SensorNodeResponseDto toDto(Sensor entity);
}
