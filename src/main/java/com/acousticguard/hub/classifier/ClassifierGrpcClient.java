package com.acousticguard.hub.classifier;

import com.acousticguard.hub.classifier.dto.ClassificationResult;
import com.acousticguard.hub.common.enums.ThreatType;
import com.acousticguard.hub.grpc.classifier.v1.AudioClassifierGrpc;
import com.acousticguard.hub.grpc.classifier.v1.AudioFrameRequest;
import com.acousticguard.hub.grpc.classifier.v1.ClassificationResponse;
import com.acousticguard.hub.sensor.dto.AudioFrame;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClassifierGrpcClient implements ClassifierClient {

    private final AudioClassifierGrpc.AudioClassifierBlockingStub classifierStub;

    @Override
    public ClassificationResult classify(AudioFrame frame) {
        try {
            AudioFrameRequest request = AudioFrameRequest.newBuilder()
                    .setSensorId(frame.sensorId() != null ? frame.sensorId() : "unknown")
                    .setCapturedAtMs(frame.capturedAtMs())
                    .setLatitude(frame.latitude())
                    .setLongitude(frame.longitude())
                    .addAllFftBins(frame.fftBins() != null ? frame.fftBins() : java.util.List.of())
                    .setSampleRateHz(frame.sampleRateHz())
                    .setPeakDb(frame.peakDb() != null ? frame.peakDb() : 0.0f)
                    .build();

            ClassificationResponse response = classifierStub.classify(request);

            ThreatType type = ThreatType.BACKGROUND;
            try {
                type = ThreatType.valueOf(response.getThreatType().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Received unknown threat type: {}", response.getThreatType());
            }

            return new ClassificationResult(type, response.getConfidence(), response.getModelVer());

        } catch (Exception e) {
            log.warn("gRPC Python service unavailable. Using MOCK classification for sensor: {}", frame.sensorId());
            return new ClassificationResult(ThreatType.UAV, 0.95f, "mock-v1");
        }
    }
}
