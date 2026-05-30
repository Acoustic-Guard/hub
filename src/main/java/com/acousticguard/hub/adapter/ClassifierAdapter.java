package com.acousticguard.hub.adapter;

import com.acousticguard.hub.classifier.ClassifierGrpcClient;
import com.acousticguard.hub.classifier.dto.ClassificationResult;
import com.acousticguard.hub.port.ClassifierPort;
import com.acousticguard.hub.sensor.dto.AudioFrame;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Adapter implementation for ClassifierPort.
 * Bridges the business logic with the gRPC classifier client.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClassifierAdapter implements ClassifierPort {

    private final ClassifierGrpcClient classifierGrpcClient;

    @Override
    public ClassificationResult classify(AudioFrame frame) {
        log.debug("Classifying audio frame from sensor {}", frame.sensorId());
        return classifierGrpcClient.classify(frame);
    }
}
