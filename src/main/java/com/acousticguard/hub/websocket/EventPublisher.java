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
public class EventPublisher implements EventPublisherPort {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void publishAlert(Object alert) {
        messagingTemplate.convertAndSend("/topic/alerts", alert);
        log.debug("Published alert to /topic/alerts");
    }

    @Override
    public void publishIncident(Object incident) {
        messagingTemplate.convertAndSend("/topic/incidents", incident);
        log.debug("Published incident to /topic/incidents");
    }

    @Override
    public void publishSensorStatus(Object sensor) {
        messagingTemplate.convertAndSend("/topic/telemetry", sensor);
        log.debug("Published sensor status to /topic/telemetry");
    }
}
