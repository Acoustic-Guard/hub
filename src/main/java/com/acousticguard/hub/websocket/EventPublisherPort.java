package com.acousticguard.hub.websocket;

/**
 * Port interface for event publishing.
 * Allows services to publish events without depending on concrete WebSocket implementation.
 */
public interface EventPublisherPort {

    /**
     * Publishes an alert event.
     *
     * @param alert the alert to publish
     */
    void publishAlert(Object alert);

    /**
     * Publishes an incident event.
     *
     * @param incident the incident to publish
     */
    void publishIncident(Object incident);

    /**
     * Publishes a sensor status event.
     *
     * @param sensor the sensor status to publish
     */
    void publishSensorStatus(Object sensor);
}
