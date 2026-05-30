package com.acousticguard.hub.adapter;

import com.acousticguard.hub.port.EventPublisherPort;
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
        log.debug("Publishing event to all clients: {}", event.getClass().getSimpleName());
        // TODO: Implement actual publishing logic in EventPublisher
        // eventPublisher.broadcast(event);
    }

    @Override
    public void publishToClient(Object event, String clientId) {
        log.debug("Publishing event to client {}: {}", clientId, event.getClass().getSimpleName());
        // TODO: Implement client-specific publishing logic in EventPublisher
        // eventPublisher.sendToClient(event, clientId);
    }
}
