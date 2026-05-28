package com.acousticguard.hub.messaging.consumer;

import com.acousticguard.hub.sensor.dto.AudioFrame;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class AudioFrameConsumer {

    private final AudioFrameService audioFrameService;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "q.frames")
    public void receiveFrame(Message message) {
        try {
            AudioFrame frame = objectMapper.readValue(message.getBody(), AudioFrame.class);
            audioFrameService.processFrame(frame);
        } catch (Exception e) {
            String sensorId = message.getMessageProperties().getHeader("x-sensor-id");
            log.error("Failed to process audio frame message. Sensor ID: {}", sensorId, e);
        }
    }
}