package com.acousticguard.hub.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Publisher for domain events to WebSocket clients via STOMP.
 * Broadcasts events to topics /topic/alerts and /topic/telemetry.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Broadcasts an alert to all subscribed clients.
     * 
     * @param alert the alert to broadcast
     */
    public void publishAlert(Object alert) {
        messagingTemplate.convertAndSend("/topic/alerts", alert);
        log.debug("Published alert to /topic/alerts");
    }

    /**
     * Broadcasts an incident to all subscribed clients.
     * 
     * @param incident the incident to broadcast
     */
    public void publishIncident(Object incident) {
        messagingTemplate.convertAndSend("/topic/incidents", incident);
        log.debug("Published incident to /topic/incidents");
    }

    /**
     * Broadcasts telemetry data to all subscribed clients.
     * 
     * @param telemetry the telemetry data to broadcast
     */
    public void publishTelemetry(Object telemetry) {
        messagingTemplate.convertAndSend("/topic/telemetry", telemetry);
        log.debug("Published telemetry to /topic/telemetry");
    }

    /**
     * Broadcasts sensor status updates to all subscribed clients.
     * 
     * @param sensor the sensor status update
     */
    public void publishSensorStatus(Object sensor) {
        messagingTemplate.convertAndSend("/topic/telemetry", sensor);
        log.debug("Published sensor status to /topic/telemetry");
    }
}
