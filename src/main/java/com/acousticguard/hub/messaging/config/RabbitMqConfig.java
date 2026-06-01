package com.acousticguard.hub.messaging.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String EXCHANGE_NAME = "acoustic.frames";
    public static final String QUEUE_FRAMES = "q.frames";
    public static final String QUEUE_HEARTBEATS = "q.heartbeats";

    @Bean
    public TopicExchange framesExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue framesQueue() {
        // true ensures the queue survives broker restarts
        return new Queue(QUEUE_FRAMES, true);
    }

    @Bean
    public Queue heartbeatsQueue() {
        return new Queue(QUEUE_HEARTBEATS, true);
    }

    @Bean
    public Binding framesBinding(Queue framesQueue, TopicExchange framesExchange) {
        // Routes messages with routing keys like 'sensor.anomaly.test'
        return BindingBuilder.bind(framesQueue).to(framesExchange).with("sensor.anomaly.*");
    }

    @Bean
    public Binding heartbeatsBinding(Queue heartbeatsQueue, TopicExchange framesExchange) {
        // Routes messages with routing keys like 'sensor.telemetry.test'
        return BindingBuilder.bind(heartbeatsQueue).to(framesExchange).with("sensor.telemetry.*");
    }
}