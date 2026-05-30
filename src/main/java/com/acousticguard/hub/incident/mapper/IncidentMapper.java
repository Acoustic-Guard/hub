package com.acousticguard.hub.incident.mapper;

import com.acousticguard.hub.common.config.MapperConfig;
import com.acousticguard.hub.incident.dto.IncidentResponseDto;
import com.acousticguard.hub.incident.model.Incident;
import org.locationtech.jts.geom.Point;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(config = MapperConfig.class)
public interface IncidentMapper {

    @Mapping(source = "locationGeo", target = "latitude", qualifiedByName = "pointToLatitude")
    @Mapping(source = "locationGeo", target = "longitude", qualifiedByName = "pointToLongitude")
    IncidentResponseDto toDto(Incident entity);

    @Named("pointToLatitude")
    default Float pointToLatitude(Point point) {
        return point != null ? (float) point.getY() : null;
    }

    @Named("pointToLongitude")
    default Float pointToLongitude(Point point) {
        return point != null ? (float) point.getX() : null;
    }
}