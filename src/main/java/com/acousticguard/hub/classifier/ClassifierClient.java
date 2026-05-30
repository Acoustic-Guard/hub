package com.acousticguard.hub.classifier;

import com.acousticguard.hub.classifier.dto.ClassificationResult;
import com.acousticguard.hub.sensor.dto.AudioFrame;

/**
 * Interface for classifier client.
 * Allows services to depend on abstraction instead of concrete implementation.
 */
public interface ClassifierClient {

    /**
     * Classifies an audio frame.
     * 
     * @param frame the audio frame to classify
     * @return the classification result
     */
    ClassificationResult classify(AudioFrame frame);
}
