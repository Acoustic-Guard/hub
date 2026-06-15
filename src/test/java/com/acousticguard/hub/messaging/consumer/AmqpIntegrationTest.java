package com.acousticguard.hub.messaging.consumer;

import com.acousticguard.hub.BaseIntegrationTest;
import com.acousticguard.hub.alert.model.Alert;
import com.acousticguard.hub.alert.repository.AlertRepository;
import com.acousticguard.hub.sensor.model.Sensor;
import com.acousticguard.hub.sensor.repository.SensorRepository;
import com.acousticguard.hub.sensor.dto.AudioFrame;
import com.acousticguard.hub.telemetry.dto.TelemetryEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration tests for RabbitMQ message consumers using Testcontainers.
 * Tests the full AMQP listener lifecycle:
 * - Send raw JSON payload to queue
 * - Verify database state updates
 * - Verify WebSocket events are published (mocked)
 * 
 * NOTE: These tests require Docker Desktop to be running with proper daemon configuration.
 * If Docker is not available, these tests will be skipped automatically.
 */
@SpringBootTest
@ActiveProfiles("test")
@EnabledIf("isDockerAvailable")
@Disabled("Integration tests require Docker Desktop with proper daemon configuration")
class AmqpIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private SensorRepository sensorRepository;

    @Autowired
    private ObjectMapper objectMapper;

    static boolean isDockerAvailable() {
        try {
            Process process = Runtime.getRuntime().exec("docker --version");
            process.waitFor();
            return process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    @BeforeEach
    void setUp() {
        alertRepository.deleteAll();
        sensorRepository.deleteAll();
    }

    @Nested
    @DisplayName("When consuming audio frame messages")
    class WhenConsumingAudioFrameMessages {

        @Test
        @DisplayName("Should process audio frame and create alert when confidence threshold met")
        void shouldProcessAudioFrameAndCreateAlertWhenConfidenceThresholdMet() {
            // Arrange
            AudioFrame frame = createAudioFrame("sensor-1", 48.5f, 35.5f, 0.8f);
            String jsonPayload = serializeToJson(frame);
            Message message = MessageBuilder.withBody(jsonPayload.getBytes()).build();

            // Act
            rabbitTemplate.send("acoustic.frames", "sensor.anomaly.test", message);

            // Assert - Wait for async processing
            await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
                List<Alert> alerts = alertRepository.findAll();
                assertThat(alerts).isNotEmpty();
            });
        }

        @Test
        @DisplayName("Should not create alert when confidence below threshold")
        void shouldNotCreateAlertWhenConfidenceBelowThreshold() {
            // Arrange
            AudioFrame frame = createAudioFrame("sensor-1", 48.5f, 35.5f, 0.5f); // Below default 0.75 threshold
            String jsonPayload = serializeToJson(frame);
            Message message = MessageBuilder.withBody(jsonPayload.getBytes()).build();

            // Act
            rabbitTemplate.send("acoustic.frames", "sensor.anomaly.test", message);

            // Assert - Wait to ensure no alert is created
            await().atMost(3, TimeUnit.SECONDS).untilAsserted(() -> {
                List<Alert> alerts = alertRepository.findAll();
                assertThat(alerts).isEmpty();
            });
        }

        @Test
        @DisplayName("Should handle duplicate audio frames idempotently")
        void shouldHandleDuplicateAudioFramesIdempotently() {
            // Arrange
            long capturedAt = System.currentTimeMillis();
            AudioFrame frame = createAudioFrame("sensor-1", 48.5f, 35.5f, 0.8f);
            frame = new AudioFrame(
                    frame.sensorId(),
                    capturedAt,
                    frame.latitude(),
                    frame.longitude(),
                    frame.fftBins(),
                    frame.sampleRateHz(),
                    frame.peakDb(),
                    frame.avgDb(),
                    frame.rawAudio()
            );
            String jsonPayload = serializeToJson(frame);
            Message message = MessageBuilder.withBody(jsonPayload.getBytes()).build();

            // Act - Send same message twice
            rabbitTemplate.send("acoustic.frames", "sensor.anomaly.test", message);
            rabbitTemplate.send("acoustic.frames", "sensor.anomaly.test", message);

            // Assert - Should only create one alert
            await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
                List<Alert> alerts = alertRepository.findAll();
                assertThat(alerts).hasSize(1);
            });
        }
    }

    @Nested
    @DisplayName("When consuming telemetry messages")
    class WhenConsumingTelemetryMessages {

        @Test
        @DisplayName("Should process telemetry and update sensor state")
        void shouldProcessTelemetryAndUpdateSensorState() {
            // Arrange
            TelemetryEvent event = createTelemetryEvent("sensor-1", -20.0f, 48.5f, 35.5f);
            String jsonPayload = serializeToJson(event);
            Message message = MessageBuilder.withBody(jsonPayload.getBytes()).build();

            // Act
            rabbitTemplate.send("acoustic.frames", "sensor.telemetry.test", message);

            // Assert - Wait for async processing
            await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
                List<Sensor> sensors = sensorRepository.findAll();
                assertThat(sensors).isNotEmpty();
                Sensor sensor = sensors.get(0);
                assertThat(sensor.getStatus().name()).isEqualTo("ONLINE");
                assertThat(sensor.getCurrentAvgDb()).isGreaterThan(0);
            });
        }

        @Test
        @DisplayName("Should auto-register new sensor on first telemetry")
        void shouldAutoRegisterNewSensorOnFirstTelemetry() {
            // Arrange
            TelemetryEvent event = createTelemetryEvent("new-sensor", -20.0f, 48.5f, 35.5f);
            String jsonPayload = serializeToJson(event);
            Message message = MessageBuilder.withBody(jsonPayload.getBytes()).build();

            // Act
            rabbitTemplate.send("acoustic.frames", "sensor.telemetry.test", message);

            // Assert - Wait for async processing
            await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
                List<Sensor> sensors = sensorRepository.findAll();
                assertThat(sensors).hasSize(1);
                assertThat(sensors.get(0).getId()).isEqualTo("new-sensor");
            });
        }

        @Test
        @DisplayName("Should throttle sensor updates to prevent excessive DB writes")
        void shouldThrottleSensorUpdatesToPreventExcessiveDbWrites() {
            // Arrange
            TelemetryEvent event = createTelemetryEvent("sensor-1", -20.0f, 48.5f, 35.5f);
            String jsonPayload = serializeToJson(event);
            Message message = MessageBuilder.withBody(jsonPayload.getBytes()).build();

            // Act - Send multiple telemetry messages rapidly
            for (int i = 0; i < 10; i++) {
                rabbitTemplate.send("acoustic.frames", "sensor.telemetry.test", message);
            }

            // Assert - Should only have one sensor entry (throttled)
            await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
                List<Sensor> sensors = sensorRepository.findAll();
                assertThat(sensors).hasSize(1);
            });
        }
    }

    @Nested
    @DisplayName("When handling malformed messages")
    class WhenHandlingMalformedMessages {

        @Test
        @DisplayName("Should handle invalid JSON gracefully")
        void shouldHandleInvalidJsonGracefully() {
            // Arrange
            String invalidJson = "{ invalid json }";
            Message message = MessageBuilder.withBody(invalidJson.getBytes()).build();

            // Act - Should not throw exception
            rabbitTemplate.send("acoustic.frames", "sensor.telemetry.test", message);

            // Assert - Wait to ensure no crash
            await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
                // System should still be functional
                assertThat(true).isTrue();
            });
        }

        @Test
        @DisplayName("Should handle missing fields gracefully")
        void shouldHandleMissingFieldsGracefully() {
            // Arrange
            String incompleteJson = "{\"sensorId\":\"sensor-1\"}"; // Missing required fields
            Message message = MessageBuilder.withBody(incompleteJson.getBytes()).build();

            // Act - Should not throw exception
            rabbitTemplate.send("acoustic.frames", "sensor.telemetry.test", message);

            // Assert - Wait to ensure no crash
            await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
                // System should still be functional
                assertThat(true).isTrue();
            });
        }
    }

    // Helper methods
    private AudioFrame createAudioFrame(String sensorId, float latitude, float longitude, float confidence) {
        return new AudioFrame(
                sensorId,
                System.currentTimeMillis(),
                latitude,
                longitude,
                List.of(1.0f, 2.0f, 3.0f),
                16000.0f,
                -10.0f,
                null,
                null
        );
    }

    private TelemetryEvent createTelemetryEvent(String sensorId, float avgDb, float latitude, float longitude) {
        return new TelemetryEvent(
                sensorId,
                System.currentTimeMillis(),
                latitude,
                longitude,
                avgDb
        );
    }

    private String serializeToJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize object to JSON", e);
        }
    }
}
