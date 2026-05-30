package com.acousticguard.hub.adapter;

import com.acousticguard.hub.port.AudioFramePort;
import com.acousticguard.hub.sensor.dto.AudioFrame;
import com.acousticguard.hub.sensor.service.AudioFrameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Adapter implementation for AudioFramePort.
 * Bridges the inbound RabbitMQ messages with the business logic.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AudioFrameAdapter implements AudioFramePort {

    private final AudioFrameService audioFrameService;

    @Override
    public void deliver(AudioFrame frame) {
        log.debug("Delivering audio frame from sensor {}", frame.sensorId());
        audioFrameService.processFrame(frame);
    }
}
