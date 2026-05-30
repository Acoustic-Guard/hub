package com.acousticguard.hub.port;

/**
 * Port for publishing domain events to connected clients.
 * This is an outbound port that delivers events via WebSocket or similar mechanisms.
 */
public interface EventPublisherPort {
    
    /**
     * Publishes a domain event to all connected clients.
     * 
     * @param event the domain event to publish
     */
    void publish(Object event);
    
    /**
     * Publishes a domain event to a specific client.
     * 
     * @param event the domain event to publish
     * @param clientId the identifier of the target client
     */
    void publishToClient(Object event, String clientId);
}
