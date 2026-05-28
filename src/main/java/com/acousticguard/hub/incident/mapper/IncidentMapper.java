package com.acousticguard.hub.incident.mapper;

import com.acousticguard.hub.common.config.MapperConfig;
import com.acousticguard.hub.incident.dto.IncidentResponseDto;
import com.acousticguard.hub.incident.model.Incident;
import org.mapstruct.Mapper;

@Mapper(config = MapperConfig.class)
public interface IncidentMapper {

    IncidentResponseDto toDto(Incident entity);
}