package com.acousticguard.hub.websocket;

import com.acousticguard.hub.alert.dto.AlertResponseDto;
import com.acousticguard.hub.common.constant.WebSocketTopics;
import com.acousticguard.hub.incident.dto.IncidentResponseDto;
import com.acousticguard.hub.telemetry.dto.SensorNodeResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublisher implements EventPublisherPort {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void publishAlert(AlertResponseDto alert) {
        messagingTemplate.convertAndSend(WebSocketTopics.ALERTS, alert);
        log.debug("Published alert to {}", WebSocketTopics.ALERTS);
    }

    @Override
    public void publishIncident(IncidentResponseDto incident) {
        messagingTemplate.convertAndSend(WebSocketTopics.INCIDENTS, incident);
        log.debug("Published incident to {}", WebSocketTopics.INCIDENTS);
    }

    @Override
    public void publishSensorStatus(SensorNodeResponseDto sensor) {
        messagingTemplate.convertAndSend(WebSocketTopics.TELEMETRY, sensor);
        log.debug("Published sensor status to {}", WebSocketTopics.TELEMETRY);
    }
}
