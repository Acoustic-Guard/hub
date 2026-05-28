package com.acousticguard.hub.sensor.service;

import com.acousticguard.hub.classifier.ClassifierGrpcClient;
import com.acousticguard.hub.classifier.dto.ClassificationResult;
import com.acousticguard.hub.common.enums.ThreatType;
import com.acousticguard.hub.sensor.dto.AudioFrame;
// import com.acousticguard.hub.alert.service.AlertService;
// import com.acousticguard.hub.incident.service.IncidentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AudioFrameService {

    private final ClassifierGrpcClient classifierGrpcClient;
    // private final AlertService alertService;
    // private final IncidentService incidentService;

    @Value("${acoustic.classifier.confidence-threshold:0.75}")
    private float confidenceThreshold;

    public void processFrame(AudioFrame frame) {
        log.debug("Processing frame from sensor: {}", frame.sensorId());

        ClassificationResult result = classifierGrpcClient.classify(frame);

        if (result.threatType() == ThreatType.BACKGROUND) {
            return;
        }

        if (result.confidence() < confidenceThreshold) {
            log.debug("Threat detected but confidence {} is below threshold", result.confidence());
            return;
        }

        log.warn("Threat {} detected by sensor {} with confidence {}", 
                 result.threatType(), frame.sensorId(), result.confidence());

        // 4. Створення Alert
        // Alert alert = alertService.createAlert(frame, result);
        
        // 5. Агрегація в Incident (просторовий пошук)
        // incidentService.aggregateOrUpdate(alert);
    }
}