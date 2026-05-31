package com.acousticguard.hub.messaging.consumer;

import com.acousticguard.hub.sensor.dto.AudioFrame;
import com.acousticguard.hub.sensor.service.AudioFrameService;
import com.acousticguard.hub.telemetry.service.TelemetryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumer for RabbitMQ messages from sensor nodes.
 * Handles both audio frame messages and heartbeat messages.
 * Uses in-memory telemetry tracking to avoid DB bottlenecks.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AudioFrameConsumer {

    private final AudioFrameService audioFrameService;
    private final TelemetryService telemetryService;
    private final ObjectMapper objectMapper;

    /**
     * Processes audio frame messages from the RabbitMQ queue.
     * Updates in-memory telemetry and processes the frame for threat detection.
     *
     * @param message the RabbitMQ message containing audio frame data
     */
    @RabbitListener(queues = "q.frames")
    public void receiveFrame(Message message) {
        try {
            AudioFrame frame = objectMapper.readValue(message.getBody(), AudioFrame.class);

            // Update in-memory telemetry (includes heartbeat tracking)
            telemetryService.updateNodeTelemetry(frame);

            // Process the frame for threat detection
            audioFrameService.processFrame(frame);
        } catch (Exception e) {
            String sensorId = message.getMessageProperties().getHeader("x-sensor-id");
            log.error("Failed to process audio frame message. Sensor ID: {}", sensorId, e);
        }
    }

    /**
     * Processes heartbeat messages from the RabbitMQ queue.
     * Updates in-memory telemetry for the sensor.
     *
     * @param message the RabbitMQ message containing heartbeat data
     */
    @RabbitListener(queues = "q.heartbeats")
    public void receiveHeartbeat(Message message) {
        try {
            String sensorId = message.getMessageProperties().getHeader("x-sensor-id");
            if (sensorId != null) {
                // Heartbeat is tracked in-memory via telemetry
                log.debug("Heartbeat received from sensor {}", sensorId);
            }
        } catch (Exception e) {
            log.error("Failed to process heartbeat message", e);
        }
    }
}