package com.acousticguard.hub.port;

import com.acousticguard.hub.classifier.dto.ClassificationResult;
import com.acousticguard.hub.sensor.dto.AudioFrame;

/**
 * Port for calling the classifier service.
 * This is an outbound port that delegates classification to external systems (e.g., Python classifier via gRPC).
 */
public interface ClassifierPort {
    
    /**
     * Classifies an audio frame to detect potential threats.
     * 
     * @param frame the audio frame to classify
     * @return the classification result containing threat type and confidence
     */
    ClassificationResult classify(AudioFrame frame);
}
