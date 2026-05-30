package com.acousticguard.hub.classifier;

import com.acousticguard.hub.classifier.dto.ClassificationResult;
import com.acousticguard.hub.common.enums.ThreatType;
import com.acousticguard.hub.sensor.dto.AudioFrame;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.concurrent.TimeUnit;

/**
 * gRPC client for communicating with the Python classifier service.
 * Sends audio frames for classification and receives threat detection results.
 */
@Slf4j
@Component
public class ClassifierGrpcClient {

    private final ManagedChannel channel;

    @Value("${acoustic.classifier.host:localhost}")
    private String classifierHost;

    @Value("${acoustic.classifier.port:50051}")
    private int classifierPort;

    public ClassifierGrpcClient() {
        // Channel will be built after properties are injected
        this.channel = null;
    }

    /**
     * Classifies an audio frame by calling the Python classifier service via gRPC.
     * 
     * @param frame the audio frame to classify
     * @return the classification result containing threat type and confidence
     */
    public ClassificationResult classify(AudioFrame frame) {
        try {
            // TODO: Implement actual gRPC call to Python classifier
            // This requires:
            // 1. Generate protobuf classes from .proto file
            // 2. Build request with audio frame data
            // 3. Call the classifier service
            // 4. Parse response and convert to ClassificationResult
            
            log.debug("Classifying frame from sensor {} via gRPC", frame.sensorId());
            
            // Placeholder implementation - replace with actual gRPC call
            return new ClassificationResult(ThreatType.BACKGROUND, 0.9f, "mock-v1");
            
        } catch (Exception e) {
            log.error("Failed to classify frame via gRPC", e);
            return new ClassificationResult(ThreatType.BACKGROUND, 0.0f, "error");
        }
    }

    /**
     * Initializes the gRPC channel after Spring properties are set.
     */
    public void init() {
        this.channel = ManagedChannelBuilder.forAddress(classifierHost, classifierPort)
                .usePlaintext()
                .build();
        log.info("gRPC channel initialized for {}:{}", classifierHost, classifierPort);
    }

    /**
     * Shuts down the gRPC channel gracefully.
     */
    @PreDestroy
    public void shutdown() {
        if (channel != null) {
            try {
                channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
                log.info("gRPC channel shut down gracefully");
            } catch (InterruptedException e) {
                log.error("Interrupted while shutting down gRPC channel", e);
                Thread.currentThread().interrupt();
            }
        }
    }
}