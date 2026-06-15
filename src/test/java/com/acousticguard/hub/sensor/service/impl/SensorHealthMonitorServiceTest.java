package com.acousticguard.hub.sensor.service.impl;

import com.acousticguard.hub.common.enums.SensorStatus;
import com.acousticguard.hub.sensor.mapper.SensorMapper;
import com.acousticguard.hub.sensor.model.Sensor;
import com.acousticguard.hub.sensor.repository.SensorRepository;
import com.acousticguard.hub.telemetry.dto.SensorNodeResponseDto;
import com.acousticguard.hub.telemetry.service.TelemetryService;
import com.acousticguard.hub.websocket.EventPublisherPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SensorHealthMonitorService Tests")
class SensorHealthMonitorServiceTest {

    @Mock
    private SensorRepository sensorRepository;

    @Mock
    private TelemetryService telemetryService;

    @Mock
    private EventPublisherPort eventPublisherPort;

    @Mock
    private SensorMapper sensorMapper;

    @InjectMocks
    private SensorHealthMonitorServiceImpl sensorHealthMonitorService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(sensorHealthMonitorService, "heartbeatTimeoutSeconds", 60L);
    }

    @Nested
    @DisplayName("When updating heartbeat")
    class WhenUpdatingHeartbeat {

        @Test
        @DisplayName("Should track heartbeat in-memory without DB write")
        void shouldTrackHeartbeatInMemoryWithoutDbWrite() {
            // Act
            sensorHealthMonitorService.updateHeartbeat("sensor-1");

            // Assert - Should not interact with repository
            verify(sensorRepository, never()).save(any());
            verify(sensorRepository, never()).findById(any());
        }
    }

    @Nested
    @DisplayName("When checking offline sensors")
    class WhenCheckingOfflineSensors {

        @Test
        @DisplayName("Should mark sensor as OFFLINE when telemetry reports offline")
        void shouldMarkSensorAsOfflineWhenTelemetryReportsOffline() {
            // Arrange
            Sensor sensor = createSensor("sensor-1", SensorStatus.ONLINE);
            when(sensorRepository.findAll()).thenReturn(List.of(sensor));
            when(telemetryService.getSensorStatus("sensor-1")).thenReturn(SensorStatus.OFFLINE);
            when(sensorRepository.save(any(Sensor.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(sensorMapper.toDto(any(Sensor.class))).thenReturn(createSensorDto("sensor-1", SensorStatus.OFFLINE));

            // Act
            List<Sensor> result = sensorHealthMonitorService.checkOfflineSensors();

            // Assert
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo(SensorStatus.OFFLINE);
            verify(sensorRepository).save(any(Sensor.class));
            verify(eventPublisherPort).publishSensorStatus(any(SensorNodeResponseDto.class));
        }

        @Test
        @DisplayName("Should mark sensor as ONLINE when telemetry reports online")
        void shouldMarkSensorAsOnlineWhenTelemetryReportsOnline() {
            // Arrange
            Sensor sensor = createSensor("sensor-1", SensorStatus.OFFLINE);
            when(sensorRepository.findAll()).thenReturn(List.of(sensor));
            when(telemetryService.getSensorStatus("sensor-1")).thenReturn(SensorStatus.ONLINE);
            when(telemetryService.getSensorLatency("sensor-1")).thenReturn(100L);
            when(sensorRepository.save(any(Sensor.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(sensorMapper.toDto(any(Sensor.class))).thenReturn(createSensorDto("sensor-1", SensorStatus.ONLINE));

            // Act
            List<Sensor> result = sensorHealthMonitorService.checkOfflineSensors();

            // Assert
            assertThat(result).hasSize(0); // No offline sensors returned
            verify(sensorRepository).save(any(Sensor.class));
            verify(eventPublisherPort).publishSensorStatus(any(SensorNodeResponseDto.class));
        }

        @Test
        @DisplayName("Should not update status when status unchanged")
        void shouldNotUpdateStatusWhenStatusUnchanged() {
            // Arrange
            Sensor sensor = createSensor("sensor-1", SensorStatus.ONLINE);
            when(sensorRepository.findAll()).thenReturn(List.of(sensor));
            when(telemetryService.getSensorStatus("sensor-1")).thenReturn(SensorStatus.ONLINE);

            // Act
            List<Sensor> result = sensorHealthMonitorService.checkOfflineSensors();

            // Assert
            assertThat(result).isEmpty();
            verify(sensorRepository, never()).save(any(Sensor.class));
            verify(eventPublisherPort, never()).publishSensorStatus(any());
        }

        @Test
        @DisplayName("Should include latency in DTO when sensor comes online")
        void shouldIncludeLatencyInDtoWhenSensorComesOnline() {
            // Arrange
            Sensor sensor = createSensor("sensor-1", SensorStatus.OFFLINE);
            when(sensorRepository.findAll()).thenReturn(List.of(sensor));
            when(telemetryService.getSensorStatus("sensor-1")).thenReturn(SensorStatus.ONLINE);
            when(telemetryService.getSensorLatency("sensor-1")).thenReturn(150L);
            when(sensorRepository.save(any(Sensor.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(sensorMapper.toDto(any(Sensor.class))).thenReturn(createSensorDto("sensor-1", SensorStatus.ONLINE));

            // Act
            sensorHealthMonitorService.checkOfflineSensors();

            // Assert
            ArgumentCaptor<SensorNodeResponseDto> dtoCaptor = ArgumentCaptor.forClass(SensorNodeResponseDto.class);
            verify(eventPublisherPort).publishSensorStatus(dtoCaptor.capture());
            assertThat(dtoCaptor.getValue().latencyMs()).isEqualTo(150);
        }

        @Test
        @DisplayName("Should handle multiple sensors correctly")
        void shouldHandleMultipleSensorsCorrectly() {
            // Arrange
            Sensor sensor1 = createSensor("sensor-1", SensorStatus.ONLINE);
            Sensor sensor2 = createSensor("sensor-2", SensorStatus.ONLINE);
            Sensor sensor3 = createSensor("sensor-3", SensorStatus.ONLINE);
            
            when(sensorRepository.findAll()).thenReturn(List.of(sensor1, sensor2, sensor3));
            when(telemetryService.getSensorStatus("sensor-1")).thenReturn(SensorStatus.OFFLINE);
            when(telemetryService.getSensorStatus("sensor-2")).thenReturn(SensorStatus.ONLINE);
            when(telemetryService.getSensorStatus("sensor-3")).thenReturn(SensorStatus.OFFLINE);
            when(sensorRepository.save(any(Sensor.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(sensorMapper.toDto(any(Sensor.class))).thenReturn(createSensorDto("sensor-1", SensorStatus.OFFLINE));

            // Act
            List<Sensor> result = sensorHealthMonitorService.checkOfflineSensors();

            // Assert
            assertThat(result).hasSize(2); // Two sensors marked offline
            verify(sensorRepository, times(2)).save(any(Sensor.class));
        }
    }

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

    private SensorNodeResponseDto createSensorDto(String id, SensorStatus status) {
        return new SensorNodeResponseDto(
                id,
                "Test Location",
                status,
                null,
                null,
                Instant.now(),
                48.5f,
                35.5f,
                null,
                null
        );
    }
}
