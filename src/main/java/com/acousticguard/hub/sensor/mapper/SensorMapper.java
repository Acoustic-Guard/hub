package com.acousticguard.hub.sensor.mapper;

import com.acousticguard.hub.common.config.MapperConfig;
import com.acousticguard.hub.sensor.model.Sensor;
import com.acousticguard.hub.telemetry.dto.SensorNodeResponseDto;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper for Sensor entity to DTO conversion.
 * <p>
 * Provides mapping methods to convert Sensor entities to SensorNodeResponseDto objects.
 * Includes a method to update latency in an existing DTO. Uses the shared
 * MapperConfig for consistent mapping configuration across the application.
 * </p>
 */
@Mapper(config = MapperConfig.class)
public interface SensorMapper {

    /**
     * Converts a Sensor entity to a SensorNodeResponseDto.
     * <p>
     * Maps all sensor fields including status, location, and metadata to the DTO.
     * Used for serializing sensor data for API responses and WebSocket broadcasts.
     * </p>
     *
     * @param entity the Sensor entity to convert
     * @return the SensorNodeResponseDto with mapped fields
     */
    SensorNodeResponseDto toDto(Sensor entity);

    /**
     * Updates the latency field in an existing SensorNodeResponseDto.
     * <p>
     * This method is used to enrich DTOs with real-time latency data from the
     * telemetry service without reconstructing the entire DTO. Since DTOs are
     * immutable records, this method creates a new instance with the updated latency.
     * </p>
     *
     * @param dto     the existing DTO to update
     * @param latency the new latency value in milliseconds
     * @return a new SensorNodeResponseDto with updated latency
     */
    @org.mapstruct.Mapping(target = "latencyMs", source = "latency")
    SensorNodeResponseDto updateLatency(SensorNodeResponseDto dto, Integer latency);
}
