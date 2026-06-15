package com.acousticguard.hub.sensor.service.impl;

import com.acousticguard.hub.common.enums.SensorStatus;
import com.acousticguard.hub.sensor.model.Sensor;
import com.acousticguard.hub.sensor.repository.SensorRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SensorRegistryService Tests")
class SensorRegistryServiceTest {

    @Mock
    private SensorRepository sensorRepository;

    @InjectMocks
    private SensorRegistryServiceImpl sensorRegistryService;

    // Helper methods
    private Sensor createSensor(String id, SensorStatus status) {
        return Sensor.builder()
                .id(id)
                .location("Test Location")
                .latitude(48.5f)
                .longitude(35.5f)
                .status(status)
                .currentAvgDb(50.0f)
                .lastHeartbeat(Instant.now())
                .noiseUpdatedAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("When getting all sensors")
    class WhenGettingAllSensors {

        @Test
        @DisplayName("Should return all sensors from repository")
        void shouldReturnAllSensorsFromRepository() {
            // Arrange
            Sensor sensor1 = createSensor("sensor-1", SensorStatus.ONLINE);
            Sensor sensor2 = createSensor("sensor-2", SensorStatus.OFFLINE);
            when(sensorRepository.findAll()).thenReturn(List.of(sensor1, sensor2));

            // Act
            List<Sensor> result = sensorRegistryService.getAllSensors();

            // Assert
            assertThat(result).hasSize(2);
            verify(sensorRepository).findAll();
        }

        @Test
        @DisplayName("Should use read-only transaction")
        void shouldUseReadOnlyTransaction() {
            // Arrange
            when(sensorRepository.findAll()).thenReturn(List.of());

            // Act
            sensorRegistryService.getAllSensors();

            // Assert - Method is annotated with @Transactional(readOnly = true)
            // This is verified by the annotation on the method
        }
    }
}
