package com.acousticguard.hub.websocket;

import com.acousticguard.hub.alert.dto.AlertResponseDto;
import com.acousticguard.hub.incident.dto.IncidentResponseDto;
import com.acousticguard.hub.telemetry.dto.SensorNodeResponseDto;

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
    void publishAlert(AlertResponseDto alert);

    /**
     * Publishes an incident event.
     *
     * @param incident the incident to publish
     */
    void publishIncident(IncidentResponseDto incident);

    /**
     * Publishes a sensor status event.
     *
     * @param sensor the sensor status to publish
     */
    void publishSensorStatus(SensorNodeResponseDto sensor);
}
