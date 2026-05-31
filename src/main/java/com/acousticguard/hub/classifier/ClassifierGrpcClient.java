package com.acousticguard.hub.classifier;

import com.acousticguard.hub.classifier.dto.ClassificationResult;
import com.acousticguard.hub.common.enums.ThreatType;
import com.acousticguard.hub.grpc.classifier.v1.AudioClassifierGrpc;
import com.acousticguard.hub.grpc.classifier.v1.AudioFrameRequest;
import com.acousticguard.hub.grpc.classifier.v1.ClassificationResponse;
import com.acousticguard.hub.sensor.dto.AudioFrame;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class ClassifierGrpcClient implements ClassifierClient {

    private ManagedChannel channel;

    @Value("${acoustic.classifier.host:localhost}")
    private String classifierHost;

    @Value("${acoustic.classifier.port:50051}")
    private int classifierPort;

    public ClassifierGrpcClient() {
        // Channel will be built after properties are injected
        this.channel = null;
    }

    public void init() {
        this.channel = ManagedChannelBuilder.forAddress(classifierHost, classifierPort)
                .usePlaintext()
                .build();
        log.info("gRPC channel initialized for {}:{}", classifierHost, classifierPort);
    }

    @Override
    public ClassificationResult classify(AudioFrame frame) {
        try {
            if (channel == null) {
                init();
            }

            AudioClassifierGrpc.AudioClassifierBlockingStub stub = AudioClassifierGrpc.newBlockingStub(channel);

            AudioFrameRequest request = AudioFrameRequest.newBuilder()
                    .setSensorId(frame.sensorId() != null ? frame.sensorId() : "unknown")
                    .setCapturedAtMs(frame.capturedAtMs())
                    .setLatitude(frame.latitude())
                    .setLongitude(frame.longitude())
                    .addAllFftBins(frame.fftBins() != null ? frame.fftBins() : java.util.List.of())
                    .setSampleRateHz(frame.sampleRateHz())
                    .setPeakDb(frame.peakDb() != null ? frame.peakDb() : 0.0f)
                    .setAvgDb(frame.avgDb() != null ? frame.avgDb() : 0.0f)
                    .build();

            ClassificationResponse response = stub.classify(request);

            ThreatType type = ThreatType.BACKGROUND;
            try {
                type = ThreatType.valueOf(response.getThreatType().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Received unknown threat type: {}", response.getThreatType());
            }

            return new ClassificationResult(type, response.getConfidence(), response.getModelVer());

        } catch (Exception e) {
            log.error("gRPC call to Python failed for sensor: {}", frame.sensorId(), e);
            return new ClassificationResult(ThreatType.BACKGROUND, 0.0f, "error");
        }
    }

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