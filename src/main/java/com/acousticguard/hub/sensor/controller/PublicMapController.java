package com.acousticguard.hub.sensor.controller;

import com.acousticguard.hub.common.dto.NoisePointDto;
import com.acousticguard.hub.common.enums.SensorStatus;
import com.acousticguard.hub.sensor.model.Sensor;
import com.acousticguard.hub.sensor.repository.SensorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/public/noise-map")
@RequiredArgsConstructor
public class PublicMapController {

    private final SensorRepository sensorRepository;

    @GetMapping
    public ResponseEntity<List<NoisePointDto>> getNoiseMap() {
        List<Sensor> onlineSensors = sensorRepository.findByStatusAndCurrentAvgDbIsNotNull(SensorStatus.ONLINE);

        List<NoisePointDto> noisePoints = onlineSensors.stream()
                .map(this::mapToNoisePointDto)
                .toList();

        return ResponseEntity.ok(noisePoints);
    }

    private NoisePointDto mapToNoisePointDto(Sensor sensor) {
        return new NoisePointDto(
                obfuscateCoordinate(sensor.getLatitude()),
                obfuscateCoordinate(sensor.getLongitude()),
                sensor.getCurrentAvgDb()
        );
    }

    private Float obfuscateCoordinate(float coordinate) {
        return Math.round(coordinate * 100.0f) / 100.0f;
    }
}
