package com.acousticguard.hub.adapter;

import com.acousticguard.hub.alert.model.Alert;
import com.acousticguard.hub.incident.model.Incident;
import com.acousticguard.hub.port.EventPublisherPort;
import com.acousticguard.hub.sensor.model.Sensor;
import com.acousticguard.hub.websocket.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Adapter implementation for EventPublisherPort.
 * Bridges the business logic with the WebSocket event publisher.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublisherAdapter implements EventPublisherPort {

    private final EventPublisher eventPublisher;

    @Override
    public void publish(Object event) {
        if (event instanceof Alert) {
            eventPublisher.publishAlert(event);
        } else if (event instanceof Incident) {
            eventPublisher.publishIncident(event);
        } else if (event instanceof Sensor) {
            eventPublisher.publishSensorStatus(event);
        } else {
            eventPublisher.publishTelemetry(event);
        }
    }

    @Override
    public void publishToClient(Object event, String clientId) {
        log.debug("Publishing event to client {}: {}", clientId, event.getClass().getSimpleName());
        // STOMP doesn't support client-specific publishing in this simple implementation
        // All events are broadcast to topic subscribers
        publish(event);
    }
}
