package com.acousticguard.hub.port;

import com.acousticguard.hub.sensor.dto.AudioFrame;

/**
 * Port for receiving audio frames from external systems (e.g., RabbitMQ).
 * This is an inbound port that delivers AudioFrame messages to the service layer.
 */
public interface AudioFramePort {
    
    /**
     * Delivers an audio frame to the service layer for processing.
     * 
     * @param frame the audio frame containing sensor data
     */
    void deliver(AudioFrame frame);
}
