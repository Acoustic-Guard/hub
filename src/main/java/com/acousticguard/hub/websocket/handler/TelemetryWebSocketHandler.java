package com.acousticguard.hub.websocket.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * WebSocket handler for real-time telemetry and event broadcasting.
 * Manages connected client sessions and broadcasts messages to all subscribers.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TelemetryWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final CopyOnWriteArraySet<WebSocketSession> sessions = new CopyOnWriteArraySet<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        log.info("WebSocket client connected. Total connected: {}", sessions.size());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        log.info("WebSocket client disconnected. Total connected: {}", sessions.size());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        log.debug("Received message from WebSocket client: {}", message.getPayload());
    }

    /**
     * Broadcasts an event to all connected WebSocket clients.
     * 
     * @param event the event object to broadcast
     */
    public void broadcast(Object event) {
        String message;
        try {
            message = objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            log.error("Failed to serialize event for broadcast", e);
            return;
        }

        TextMessage textMessage = new TextMessage(message);
        sessions.forEach(session -> {
            if (session.isOpen()) {
                try {
                    session.sendMessage(textMessage);
                } catch (IOException e) {
                    log.error("Failed to send message to WebSocket session", e);
                }
            }
        });
    }

    /**
     * Sends an event to a specific WebSocket client.
     * 
     * @param event the event object to send
     * @param sessionId the target session ID
     */
    public void sendToClient(Object event, String sessionId) {
        sessions.stream()
                .filter(session -> session.getId().equals(sessionId))
                .filter(WebSocketSession::isOpen)
                .findFirst()
                .ifPresent(session -> {
                    try {
                        String message = objectMapper.writeValueAsString(event);
                        session.sendMessage(new TextMessage(message));
                    } catch (Exception e) {
                        log.error("Failed to send message to WebSocket session {}", sessionId, e);
                    }
                });
    }
}
