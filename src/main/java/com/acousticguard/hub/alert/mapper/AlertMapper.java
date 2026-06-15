package com.acousticguard.hub.alert.mapper;

import com.acousticguard.hub.alert.dto.AlertResponseDto;
import com.acousticguard.hub.alert.model.Alert;
import com.acousticguard.hub.common.config.MapperConfig;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/**
 * MapStruct mapper for Alert entity to DTO conversion.
 * <p>
 * Provides mapping methods to convert Alert entities to AlertResponseDto objects.
 * Handles conversion of PostGIS Point geometry to latitude/longitude coordinates.
 * Uses the shared MapperConfig for consistent mapping configuration across the application.
 * </p>
 */
@Mapper(config = MapperConfig.class)
public interface AlertMapper {

    /**
     * Converts an Alert entity to an AlertResponseDto.
     * <p>
     * Maps the PostGIS Point geometry to latitude and longitude coordinates using
     * custom named mapping methods. This enables proper serialization of geographic
     * data for API responses.
     * </p>
     *
     * @param entity the Alert entity to convert
     * @return the AlertResponseDto with mapped fields
     */
    @Mapping(source = "locationGeo", target = "latitude", qualifiedByName = "pointToLatitude")
    @Mapping(source = "locationGeo", target = "longitude", qualifiedByName = "pointToLongitude")
    AlertResponseDto toDto(Alert entity);

    /**
     * Extracts latitude from a PostGIS Point.
     * <p>
     * The Y coordinate of a Point in EPSG:4326 represents latitude.
     * Returns null if the point is null.
     * </p>
     *
     * @param point the PostGIS Point geometry
     * @return the latitude as a Float, or null if point is null
     */
    @Named("pointToLatitude")
    default Float pointToLatitude(Point point) {
        return point != null ? (float) point.getY() : null;
    }

    /**
     * Extracts longitude from a PostGIS Point.
     * <p>
     * The X coordinate of a Point in EPSG:4326 represents longitude.
     * Returns null if the point is null.
     * </p>
     *
     * @param point the PostGIS Point geometry
     * @return the longitude as a Float, or null if point is null
     */
    @Named("pointToLongitude")
    default Float pointToLongitude(Point point) {
        return point != null ? (float) point.getX() : null;
    }

    /**
     * Converts latitude and longitude coordinates to a PostGIS Point.
     * <p>
     * Creates a Point geometry in EPSG:4326 (WGS 84) coordinate system.
     * Used for spatial queries and database persistence.
     * </p>
     *
     * @param latitude  the latitude coordinate
     * @param longitude the longitude coordinate
     * @return a PostGIS Point geometry
     */
    @Named("latitudeLongitudeToPoint")
    default Point latitudeLongitudeToPoint(float latitude, float longitude) {
        GeometryFactory geometryFactory = new GeometryFactory();
        return geometryFactory.createPoint(new Coordinate(longitude, latitude));
    }
}