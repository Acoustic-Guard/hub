package com.acousticguard.hub.alert.mapper;

import com.acousticguard.hub.alert.dto.AlertResponseDto;
import com.acousticguard.hub.alert.model.Alert;
import com.acousticguard.hub.common.config.MapperConfig;
import org.mapstruct.Mapper;

@Mapper(config = MapperConfig.class)
public interface AlertMapper {

    AlertResponseDto toDto(Alert entity);
}