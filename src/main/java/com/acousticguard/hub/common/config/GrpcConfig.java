package com.acousticguard.hub.common.config;

import com.acousticguard.hub.grpc.classifier.v1.AudioClassifierGrpc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

@Configuration
public class GrpcConfig {

    @Bean
    public AudioClassifierGrpc.AudioClassifierBlockingStub audioClassifierStub(GrpcChannelFactory channels) {
        return AudioClassifierGrpc.newBlockingStub(channels.createChannel("python-classifier"));
    }
}
