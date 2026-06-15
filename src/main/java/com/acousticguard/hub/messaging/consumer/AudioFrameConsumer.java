package com.acousticguard.hub.messaging.consumer;

import com.acousticguard.hub.monitoring.MessageLoadMonitor;
import com.acousticguard.hub.sensor.dto.AudioFrame;
import com.acousticguard.hub.sensor.service.AudioFrameService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumer for RabbitMQ audio frame messages from q.frames queue.
 * Handles high-frequency data strictly for ML threat classification.
 * Single Responsibility: Route audio frames to the ML gRPC client/AlertService.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AudioFrameConsumer {

    private final AudioFrameService audioFrameService;
    private final ObjectMapper objectMapper;
    private final MessageLoadMonitor messageLoadMonitor;

    /**
     * Processes audio frame messages from the RabbitMQ q.frames queue.
     * Routes the frame to the ML classifier for threat detection.
     *
     * @param message the RabbitMQ message containing audio frame data
     */
    @RabbitListener(queues = "q.frames")
    public void receiveFrame(Message message) {
        try {
            messageLoadMonitor.incrementMessageCount();
            
            AudioFrame frame = objectMapper.readValue(message.getBody(), AudioFrame.class);

            // Process the frame for threat detection
            audioFrameService.processFrame(frame);

            log.debug("Audio frame processed for sensor: {}", frame.sensorId());
        } catch (Exception e) {
            String sensorId = message.getMessageProperties().getHeader("x-sensor-id");
            log.error("Failed to process audio frame message. Sensor ID: {}", sensorId, e);
        }
    }
}