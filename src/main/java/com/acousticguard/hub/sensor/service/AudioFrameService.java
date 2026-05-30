package com.acousticguard.hub.sensor.service;

import com.acousticguard.hub.alert.model.Alert;
import com.acousticguard.hub.alert.service.AlertService;
import com.acousticguard.hub.classifier.ClassifierGrpcClient;
import com.acousticguard.hub.classifier.dto.ClassificationResult;
import com.acousticguard.hub.common.enums.ThreatType;
import com.acousticguard.hub.incident.service.IncidentService;
import com.acousticguard.hub.sensor.dto.AudioFrame;
import com.acousticguard.hub.telemetry.service.TelemetryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service for processing audio frames from sensors.
 * Coordinates classification, alert creation, and incident aggregation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AudioFrameService {

    private final ClassifierGrpcClient classifierGrpcClient;
    private final TelemetryService telemetryService;
    private final AlertService alertService;
    private final IncidentService incidentService;

    /**
     * Processes an audio frame from a sensor.
     * Updates telemetry, classifies the frame, and creates alerts/incidents if threats are detected.
     * 
     * @param frame the audio frame to process
     */
    public void processFrame(AudioFrame frame) {
        log.debug("Processing frame from sensor: {}", frame.sensorId());

        if (frame.avgDb() != null) {
            telemetryService.updateNodeTelemetry(frame);
        }

        ClassificationResult result = classifierGrpcClient.classify(frame);

        if (result.threatType() == ThreatType.BACKGROUND) {
            return;
        }

        log.warn("Threat {} detected by sensor {} with confidence {}",
                result.threatType(), frame.sensorId(), result.confidence());

        Optional<Alert> alert = alertService.createAlert(frame, result);
        
        alert.ifPresent(a -> {
            incidentService.aggregateOrUpdate(a);
        });
    }
}