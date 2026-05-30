package com.acousticguard.hub.websocket;

import com.acousticguard.hub.websocket.handler.TelemetryWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Publisher for domain events to WebSocket clients.
 * Delegates to the WebSocket handler for actual broadcasting.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublisher {

    private final TelemetryWebSocketHandler webSocketHandler;

    /**
     * Broadcasts an event to all connected WebSocket clients.
     * 
     * @param event the event to broadcast
     */
    public void broadcast(Object event) {
        webSocketHandler.broadcast(event);
    }

    /**
     * Sends an event to a specific WebSocket client.
     * 
     * @param event the event to send
     * @param sessionId the target session ID
     */
    public void sendToClient(Object event, String sessionId) {
        webSocketHandler.sendToClient(event, sessionId);
    }
}
