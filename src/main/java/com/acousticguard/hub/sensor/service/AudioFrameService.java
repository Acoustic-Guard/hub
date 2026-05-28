package com.acousticguard.hub.sensor.service;

import com.acousticguard.hub.classifier.ClassifierGrpcClient;
import com.acousticguard.hub.classifier.dto.ClassificationResult;
import com.acousticguard.hub.common.enums.ThreatType;
import com.acousticguard.hub.sensor.dto.AudioFrame;
import com.acousticguard.hub.telemetry.service.TelemetryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AudioFrameService {

    private final ClassifierGrpcClient classifierGrpcClient;
    private final TelemetryService telemetryService;

    @Value("${acoustic.classifier.confidence-threshold:0.75}")
    private float confidenceThreshold;

    public void processFrame(AudioFrame frame) {
        log.debug("Processing frame from sensor: {}", frame.sensorId());

        if (frame.avgDb() != null) {
            telemetryService.updateNodeNoiseLevel(frame.sensorId(), frame.avgDb());
        }

        ClassificationResult result = classifierGrpcClient.classify(frame);

        if (result.threatType() == ThreatType.BACKGROUND) {
            return;
        }

        if (result.confidence() < confidenceThreshold) {
            log.debug("Threat {} detected but confidence {} is below threshold",
                    result.threatType(), result.confidence());
            return;
        }

        log.warn("Threat {} confirmed by sensor {} with confidence {}",
                result.threatType(), frame.sensorId(), result.confidence());

        // TODO: alertService.createAlert(frame, result);
        // TODO: incidentService.aggregateOrUpdate(alert);
    }
}