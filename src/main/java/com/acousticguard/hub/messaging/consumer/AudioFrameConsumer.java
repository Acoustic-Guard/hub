package com.acousticguard.hub.messaging.consumer;

import com.acousticguard.hub.adapter.AudioFrameAdapter;
import com.acousticguard.hub.sensor.dto.AudioFrame;
import com.acousticguard.hub.sensor.service.SensorMonitorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Consumer for RabbitMQ messages from sensor nodes.
 * Handles both audio frame messages and heartbeat messages.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AudioFrameConsumer {

    private final AudioFrameAdapter audioFrameAdapter;
    private final SensorMonitorService sensorMonitorService;
    private final ObjectMapper objectMapper;

    /**
     * Processes audio frame messages from the RabbitMQ queue.
     * Updates sensor heartbeat and processes the frame for threat detection.
     * 
     * @param message the RabbitMQ message containing audio frame data
     */
    @RabbitListener(queues = "q.frames")
    public void receiveFrame(Message message) {
        try {
            AudioFrame frame = objectMapper.readValue(message.getBody(), AudioFrame.class);
            
            // Update heartbeat for the sensor
            sensorMonitorService.updateHeartbeat(frame.sensorId());
            
            // Process the frame for threat detection via adapter
            audioFrameAdapter.processFrame(frame);
        } catch (Exception e) {
            String sensorId = message.getMessageProperties().getHeader("x-sensor-id");
            log.error("Failed to process audio frame message. Sensor ID: {}", sensorId, e);
        }
    }

    /**
     * Processes heartbeat messages from the RabbitMQ queue.
     * Updates the last heartbeat timestamp for the sensor.
     * 
     * @param message the RabbitMQ message containing heartbeat data
     */
    @RabbitListener(queues = "q.heartbeats")
    public void receiveHeartbeat(Message message) {
        try {
            String sensorId = message.getMessageProperties().getHeader("x-sensor-id");
            if (sensorId != null) {
                sensorMonitorService.updateHeartbeat(sensorId);
                log.debug("Heartbeat received from sensor {}", sensorId);
            }
        } catch (Exception e) {
            log.error("Failed to process heartbeat message", e);
        }
    }
}