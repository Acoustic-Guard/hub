package com.acousticguard.hub.messaging.consumer;

import com.acousticguard.hub.monitoring.MessageLoadMonitor;
import com.acousticguard.hub.telemetry.dto.TelemetryEvent;
import com.acousticguard.hub.telemetry.service.TelemetryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumer for RabbitMQ telemetry messages from q.telemetry queue.
 * Handles low-frequency data strictly for the noise map.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TelemetryConsumer {

    private final TelemetryService telemetryService;
    private final ObjectMapper objectMapper;
    private final MessageLoadMonitor messageLoadMonitor;

    /**
     * Processes telemetry messages from the RabbitMQ q.telemetry queue.
     * Updates in-memory telemetry and persists noise data to the database.
     *
     * @param message the RabbitMQ message containing telemetry data
     */
    @RabbitListener(queues = "q.telemetry")
    public void receiveTelemetry(Message message) {
        try {
            messageLoadMonitor.incrementMessageCount();

            TelemetryEvent event = objectMapper.readValue(message.getBody(), TelemetryEvent.class);

            // Update telemetry for noise map
            telemetryService.updateNodeTelemetry(event);

            log.debug("Telemetry processed for sensor: {}", event.sensorId());
        } catch (Exception e) {
            String sensorId = message.getMessageProperties().getHeader("x-sensor-id");
            log.error("Failed to process telemetry message. Sensor ID: {}", sensorId, e);
        }
    }
}
