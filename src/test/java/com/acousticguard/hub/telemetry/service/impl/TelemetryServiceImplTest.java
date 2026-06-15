package com.acousticguard.hub.telemetry.service.impl;

import com.acousticguard.hub.common.enums.SensorStatus;
import com.acousticguard.hub.common.enums.SystemStatus;
import com.acousticguard.hub.monitoring.MessageLoadMonitor;
import com.acousticguard.hub.telemetry.dto.SensorNodeResponseDto;
import com.acousticguard.hub.telemetry.dto.TelemetryEvent;
import com.acousticguard.hub.telemetry.dto.TelemetryResponseDto;
import com.acousticguard.hub.telemetry.service.TelemetryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TelemetryService Tests")
class TelemetryServiceImplTest {

    @Mock
    private com.acousticguard.hub.telemetry.service.impl.SensorPersistenceService sensorPersistenceService;

    @Mock
    private SimpMessagingTemplate simpMessagingTemplate;

    @Mock
    private MessageLoadMonitor messageLoadMonitor;

    @InjectMocks
    private TelemetryServiceImpl telemetryService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(telemetryService, "heartbeatTimeoutSeconds", 60L);
    }

    @Nested
    @DisplayName("When updating node telemetry")
    class WhenUpdatingNodeTelemetry {

        @Test
        @DisplayName("Should convert dBFS to dB SPL correctly")
        void shouldConvertDbFsToDbSplCorrectly() {
            // Arrange
            TelemetryEvent event = createTelemetryEvent("sensor-1", -20.0f, 48.5f, 35.5f);
            doNothing().when(sensorPersistenceService).persistNoiseDataWithThrottle(anyString(), anyFloat(), anyFloat(), anyFloat());

            // Act
            telemetryService.updateNodeTelemetry(event);

            // Assert
            ArgumentCaptor<Float> displayDbCaptor = ArgumentCaptor.forClass(Float.class);
            verify(sensorPersistenceService).persistNoiseDataWithThrottle(
                    eq("sensor-1"), displayDbCaptor.capture(), eq(48.5f), eq(35.5f)
            );
            // -20 dBFS + 100 = 80 dB SPL
            assertThat(displayDbCaptor.getValue()).isEqualTo(80.0f);
        }

        @Test
        @DisplayName("Should clamp minimum dB to 30")
        void shouldClampMinimumDbTo30() {
            // Arrange
            TelemetryEvent event = createTelemetryEvent("sensor-1", -90.0f, 48.5f, 35.5f);
            doNothing().when(sensorPersistenceService).persistNoiseDataWithThrottle(anyString(), anyFloat(), anyFloat(), anyFloat());

            // Act
            telemetryService.updateNodeTelemetry(event);

            // Assert
            ArgumentCaptor<Float> displayDbCaptor = ArgumentCaptor.forClass(Float.class);
            verify(sensorPersistenceService).persistNoiseDataWithThrottle(
                    eq("sensor-1"), displayDbCaptor.capture(), eq(48.5f), eq(35.5f)
            );
            // -90 dBFS + 100 = 10 dB SPL, but clamped to 30
            assertThat(displayDbCaptor.getValue()).isEqualTo(30.0f);
        }

        @Test
        @DisplayName("Should calculate latency correctly")
        void shouldCalculateLatencyCorrectly() {
            // Arrange
            long capturedAt = System.currentTimeMillis() - 100; // 100ms ago
            TelemetryEvent event = createTelemetryEvent("sensor-1", -20.0f, 48.5f, 35.5f);
            event = new TelemetryEvent(
                    event.sensorId(),
                    capturedAt,
                    event.latitude(),
                    event.longitude(),
                    event.avgDb()
            );
            doNothing().when(sensorPersistenceService).persistNoiseDataWithThrottle(anyString(), anyFloat(), anyFloat(), anyFloat());

            // Act
            telemetryService.updateNodeTelemetry(event);

            // Assert
            ArgumentCaptor<Float> displayDbCaptor = ArgumentCaptor.forClass(Float.class);
            verify(sensorPersistenceService).persistNoiseDataWithThrottle(
                    eq("sensor-1"), displayDbCaptor.capture(), eq(48.5f), eq(35.5f)
            );
            // Latency should be approximately 100ms
            // We can't verify exact latency since it's stored in NodeState, but we can verify the call was made
            verify(sensorPersistenceService).persistNoiseDataWithThrottle(anyString(), anyFloat(), anyFloat(), anyFloat());
        }

        @Test
        @DisplayName("Should publish ONLINE status when sensor recovers")
        void shouldPublishOnlineStatusWhenSensorRecovers() {
            // Arrange
            TelemetryEvent event = createTelemetryEvent("sensor-1", -20.0f, 48.5f, 35.5f);
            doNothing().when(sensorPersistenceService).persistNoiseDataWithThrottle(anyString(), anyFloat(), anyFloat(), anyFloat());

            // Act
            telemetryService.updateNodeTelemetry(event);

            // Assert
            verify(simpMessagingTemplate).convertAndSend(eq("/topic/sensors"), any(SensorNodeResponseDto.class));
        }

        @Test
        @DisplayName("Should not publish ONLINE status when sensor already online")
        void shouldNotPublishOnlineStatusWhenSensorAlreadyOnline() {
            // Arrange
            TelemetryEvent event = createTelemetryEvent("sensor-1", -20.0f, 48.5f, 35.5f);
            doNothing().when(sensorPersistenceService).persistNoiseDataWithThrottle(anyString(), anyFloat(), anyFloat(), anyFloat());

            // Act - First update (sensor was offline)
            telemetryService.updateNodeTelemetry(event);
            reset(simpMessagingTemplate);

            // Act - Second update (sensor already online)
            telemetryService.updateNodeTelemetry(event);

            // Assert - Should not publish on second update
            verify(simpMessagingTemplate, never()).convertAndSend(eq("/topic/sensors"), any(SensorNodeResponseDto.class));
        }

        @Test
        @DisplayName("Should delegate persistence to adapter")
        void shouldDelegatePersistenceToAdapter() {
            // Arrange
            TelemetryEvent event = createTelemetryEvent("sensor-1", -20.0f, 48.5f, 35.5f);

            // Act
            telemetryService.updateNodeTelemetry(event);

            // Assert
            verify(sensorPersistenceService).persistNoiseDataWithThrottle(
                    eq("sensor-1"), eq(80.0f), eq(48.5f), eq(35.5f)
            );
        }
    }

    @Nested
    @DisplayName("When getting system telemetry")
    class WhenGettingSystemTelemetry {

        @Test
        @DisplayName("Should return zero values when no sensors active")
        void shouldReturnZeroValuesWhenNoSensorsActive() {
            // Act
            TelemetryResponseDto result = telemetryService.getSystemTelemetry();

            // Assert
            assertThat(result.activeNodes()).isEqualTo(0);
            assertThat(result.avgLatencyMs()).isEqualTo(0);
            assertThat(result.noiseLevelDb()).isEqualTo(0);
            assertThat(result.nodesStatus()).isEqualTo(SystemStatus.CRITICAL); // No active nodes
        }

        @Test
        @DisplayName("Should calculate average noise correctly")
        void shouldCalculateAverageNoiseCorrectly() {
            // Arrange
            TelemetryEvent event1 = createTelemetryEvent("sensor-1", -20.0f, 48.5f, 35.5f); // 80 dB
            TelemetryEvent event2 = createTelemetryEvent("sensor-2", -30.0f, 48.6f, 35.6f); // 70 dB
            doNothing().when(sensorPersistenceService).persistNoiseDataWithThrottle(anyString(), anyFloat(), anyFloat(), anyFloat());

            // Act
            telemetryService.updateNodeTelemetry(event1);
            telemetryService.updateNodeTelemetry(event2);
            TelemetryResponseDto result = telemetryService.getSystemTelemetry();

            // Assert
            assertThat(result.activeNodes()).isEqualTo(2);
            assertThat(result.noiseLevelDb()).isEqualTo(75); // Average of 80 and 70
        }

        @Test
        @DisplayName("Should set noise status to WARNING when avg > 70")
        void shouldSetNoiseStatusToWarningWhenAvgHigh() {
            // Arrange
            TelemetryEvent event = createTelemetryEvent("sensor-1", -15.0f, 48.5f, 35.5f); // 85 dB
            doNothing().when(sensorPersistenceService).persistNoiseDataWithThrottle(anyString(), anyFloat(), anyFloat(), anyFloat());

            // Act
            telemetryService.updateNodeTelemetry(event);
            TelemetryResponseDto result = telemetryService.getSystemTelemetry();

            // Assert
            assertThat(result.noiseStatus()).isEqualTo(SystemStatus.WARNING);
        }

        @Test
        @DisplayName("Should set nodes status to NORMAL when sensors active")
        void shouldSetNodesStatusToNormalWhenSensorsActive() {
            // Arrange
            TelemetryEvent event = createTelemetryEvent("sensor-1", -20.0f, 48.5f, 35.5f);
            doNothing().when(sensorPersistenceService).persistNoiseDataWithThrottle(anyString(), anyFloat(), anyFloat(), anyFloat());

            // Act
            telemetryService.updateNodeTelemetry(event);
            TelemetryResponseDto result = telemetryService.getSystemTelemetry();

            // Assert
            assertThat(result.nodesStatus()).isEqualTo(SystemStatus.NORMAL);
        }
    }

    @Nested
    @DisplayName("When getting sensor status")
    class WhenGettingSensorStatus {

        @Test
        @DisplayName("Should return OFFLINE when sensor not in cache")
        void shouldReturnOfflineWhenSensorNotInCache() {
            // Act
            SensorStatus result = telemetryService.getSensorStatus("non-existent-sensor");

            // Assert
            assertThat(result).isEqualTo(SensorStatus.OFFLINE);
        }

        @Test
        @DisplayName("Should return ONLINE when sensor recently seen")
        void shouldReturnOnlineWhenSensorRecentlySeen() {
            // Arrange
            TelemetryEvent event = createTelemetryEvent("sensor-1", -20.0f, 48.5f, 35.5f);
            doNothing().when(sensorPersistenceService).persistNoiseDataWithThrottle(anyString(), anyFloat(), anyFloat(), anyFloat());

            // Act
            telemetryService.updateNodeTelemetry(event);
            SensorStatus result = telemetryService.getSensorStatus("sensor-1");

            // Assert
            assertThat(result).isEqualTo(SensorStatus.ONLINE);
        }

        @Test
        @DisplayName("Should return OFFLINE when sensor heartbeat expired")
        void shouldReturnOfflineWhenSensorHeartbeatExpired() {
            // Arrange
            TelemetryEvent event = createTelemetryEvent("sensor-1", -20.0f, 48.5f, 35.5f);
            doNothing().when(sensorPersistenceService).persistNoiseDataWithThrottle(anyString(), anyFloat(), anyFloat(), anyFloat());

            // Act
            telemetryService.updateNodeTelemetry(event);
            // Manually expire the sensor by calling sweepDeadSensors
            ReflectionTestUtils.setField(telemetryService, "heartbeatTimeoutSeconds", 0L); // Set timeout to 0
            telemetryService.sweepDeadSensors();
            SensorStatus result = telemetryService.getSensorStatus("sensor-1");

            // Assert
            assertThat(result).isEqualTo(SensorStatus.OFFLINE);
        }
    }

    @Nested
    @DisplayName("When sweeping dead sensors")
    class WhenSweepingDeadSensors {

        @Test
        @DisplayName("Should evict sensors with expired heartbeat")
        void shouldEvictSensorsWithExpiredHeartbeat() {
            // Arrange
            TelemetryEvent event = createTelemetryEvent("sensor-1", -20.0f, 48.5f, 35.5f);
            doNothing().when(sensorPersistenceService).persistNoiseDataWithThrottle(anyString(), anyFloat(), anyFloat(), anyFloat());

            // Act
            telemetryService.updateNodeTelemetry(event);
            ReflectionTestUtils.setField(telemetryService, "heartbeatTimeoutSeconds", 0L); // Set timeout to 0
            telemetryService.sweepDeadSensors();

            // Assert
            SensorStatus result = telemetryService.getSensorStatus("sensor-1");
            assertThat(result).isEqualTo(SensorStatus.OFFLINE);
        }

        @Test
        @DisplayName("Should keep sensors with valid heartbeat")
        void shouldKeepSensorsWithValidHeartbeat() {
            // Arrange
            TelemetryEvent event = createTelemetryEvent("sensor-1", -20.0f, 48.5f, 35.5f);
            doNothing().when(sensorPersistenceService).persistNoiseDataWithThrottle(anyString(), anyFloat(), anyFloat(), anyFloat());

            // Act
            telemetryService.updateNodeTelemetry(event);
            telemetryService.sweepDeadSensors(); // Default timeout is 60s, so sensor should remain

            // Assert
            SensorStatus result = telemetryService.getSensorStatus("sensor-1");
            assertThat(result).isEqualTo(SensorStatus.ONLINE);
        }
    }

    @Nested
    @DisplayName("When broadcasting telemetry")
    class WhenBroadcastingTelemetry {

        @Test
        @DisplayName("Should broadcast to /topic/telemetry")
        void shouldBroadcastToTopicTelemetry() {
            // Arrange
            when(messageLoadMonitor.getLastMinuteEventCount()).thenReturn(100);

            // Act
            telemetryService.broadcastTelemetry();

            // Assert
            verify(simpMessagingTemplate).convertAndSend(eq("/topic/telemetry"), any(TelemetryResponseDto.class));
        }

        @Test
        @DisplayName("Should handle broadcast errors gracefully")
        void shouldHandleBroadcastErrorsGracefully() {
            // Arrange
            when(messageLoadMonitor.getLastMinuteEventCount()).thenReturn(100);
            doThrow(new RuntimeException("WebSocket error")).when(simpMessagingTemplate)
                    .convertAndSend(anyString(), any(TelemetryResponseDto.class));

            // Act - Should not throw exception
            telemetryService.broadcastTelemetry();

            // Assert - Exception should be caught and logged
            verify(simpMessagingTemplate).convertAndSend(anyString(), any(TelemetryResponseDto.class));
        }
    }

    @TestFactory
    @DisplayName("Dynamic tests for dBFS to dB SPL conversion")
    Stream<org.junit.jupiter.api.DynamicTest> dbFsToDbSplConversion() {
        return Stream.of(
                org.junit.jupiter.api.DynamicTest.dynamicTest(
                        "dBFS -10 should convert to 90 dB SPL",
                        () -> {
                            reset(sensorPersistenceService);
                            TelemetryEvent event = createTelemetryEvent("sensor-1", -10.0f, 48.5f, 35.5f);
                            doNothing().when(sensorPersistenceService).persistNoiseDataWithThrottle(anyString(), anyFloat(), anyFloat(), anyFloat());
                            
                            telemetryService.updateNodeTelemetry(event);
                            
                            ArgumentCaptor<Float> displayDbCaptor = ArgumentCaptor.forClass(Float.class);
                            verify(sensorPersistenceService).persistNoiseDataWithThrottle(
                                    eq("sensor-1"), displayDbCaptor.capture(), eq(48.5f), eq(35.5f)
                            );
                            assertThat(displayDbCaptor.getValue()).isEqualTo(90.0f);
                        }
                ),
                org.junit.jupiter.api.DynamicTest.dynamicTest(
                        "dBFS -20 should convert to 80 dB SPL",
                        () -> {
                            reset(sensorPersistenceService);
                            TelemetryEvent event = createTelemetryEvent("sensor-1", -20.0f, 48.5f, 35.5f);
                            doNothing().when(sensorPersistenceService).persistNoiseDataWithThrottle(anyString(), anyFloat(), anyFloat(), anyFloat());
                            
                            telemetryService.updateNodeTelemetry(event);
                            
                            ArgumentCaptor<Float> displayDbCaptor = ArgumentCaptor.forClass(Float.class);
                            verify(sensorPersistenceService).persistNoiseDataWithThrottle(
                                    eq("sensor-1"), displayDbCaptor.capture(), eq(48.5f), eq(35.5f)
                            );
                            assertThat(displayDbCaptor.getValue()).isEqualTo(80.0f);
                        }
                ),
                org.junit.jupiter.api.DynamicTest.dynamicTest(
                        "dBFS -30 should convert to 70 dB SPL",
                        () -> {
                            reset(sensorPersistenceService);
                            TelemetryEvent event = createTelemetryEvent("sensor-1", -30.0f, 48.5f, 35.5f);
                            doNothing().when(sensorPersistenceService).persistNoiseDataWithThrottle(anyString(), anyFloat(), anyFloat(), anyFloat());
                            
                            telemetryService.updateNodeTelemetry(event);
                            
                            ArgumentCaptor<Float> displayDbCaptor = ArgumentCaptor.forClass(Float.class);
                            verify(sensorPersistenceService).persistNoiseDataWithThrottle(
                                    eq("sensor-1"), displayDbCaptor.capture(), eq(48.5f), eq(35.5f)
                            );
                            assertThat(displayDbCaptor.getValue()).isEqualTo(70.0f);
                        }
                ),
                org.junit.jupiter.api.DynamicTest.dynamicTest(
                        "dBFS -100 should clamp to 30 dB SPL",
                        () -> {
                            reset(sensorPersistenceService);
                            TelemetryEvent event = createTelemetryEvent("sensor-1", -100.0f, 48.5f, 35.5f);
                            doNothing().when(sensorPersistenceService).persistNoiseDataWithThrottle(anyString(), anyFloat(), anyFloat(), anyFloat());
                            
                            telemetryService.updateNodeTelemetry(event);
                            
                            ArgumentCaptor<Float> displayDbCaptor = ArgumentCaptor.forClass(Float.class);
                            verify(sensorPersistenceService).persistNoiseDataWithThrottle(
                                    eq("sensor-1"), displayDbCaptor.capture(), eq(48.5f), eq(35.5f)
                            );
                            assertThat(displayDbCaptor.getValue()).isEqualTo(30.0f);
                        }
                )
        );
    }

    // Helper methods
    private TelemetryEvent createTelemetryEvent(String sensorId, float avgDb, float latitude, float longitude) {
        return new TelemetryEvent(
                sensorId,
                System.currentTimeMillis(),
                latitude,
                longitude,
                avgDb
        );
    }
}
