package com.acousticguard.hub.alert.mapper;

import com.acousticguard.hub.alert.dto.AlertResponseDto;
import com.acousticguard.hub.alert.model.Alert;
import com.acousticguard.hub.common.config.MapperConfig;
import org.locationtech.jts.geom.Point;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(config = MapperConfig.class)
public interface AlertMapper {

    @Mapping(source = "locationGeo", target = "latitude", qualifiedByName = "pointToLatitude")
    @Mapping(source = "locationGeo", target = "longitude", qualifiedByName = "pointToLongitude")
    AlertResponseDto toDto(Alert entity);

    @Named("pointToLatitude")
    default Float pointToLatitude(Point point) {
        return point != null ? (float) point.getY() : null;
    }

    @Named("pointToLongitude")
    default Float pointToLongitude(Point point) {
        return point != null ? (float) point.getX() : null;
    }
}