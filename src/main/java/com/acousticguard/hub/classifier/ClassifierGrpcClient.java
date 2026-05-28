package com.acousticguard.hub.classifier;

import com.acousticguard.hub.classifier.dto.ClassificationResult;
import com.acousticguard.hub.common.enums.ThreatType;
import com.acousticguard.hub.sensor.dto.AudioFrame;
import org.springframework.stereotype.Component;

@Component
public class ClassifierGrpcClient {

    public ClassificationResult classify(AudioFrame frame) {
        // TODO: Реалізувати виклик gRPC до Python (п. 5.2 специфікації)
        return new ClassificationResult(ThreatType.BACKGROUND, 0.9f, "mock-v1");
    }
}