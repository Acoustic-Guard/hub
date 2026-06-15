package com.acousticguard.hub.monitoring;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory monitor for tracking RabbitMQ message throughput.
 * Counts incoming messages and provides Events Per Minute (EPM) metric.
 */
@Slf4j
@Component
public class MessageLoadMonitor {

    private final AtomicInteger messageCounter = new AtomicInteger(0);
    private volatile int lastMinuteEventCount = 0;

    /**
     * Increments the message counter. Should be called by RabbitMQ listeners.
     */
    public void incrementMessageCount() {
        messageCounter.incrementAndGet();
    }

    /**
     * Gets the events per minute (EPM) for the last completed minute.
     *
     * @return number of events processed in the last minute
     */
    public int getLastMinuteEventCount() {
        return lastMinuteEventCount;
    }

    /**
     * Scheduled task that runs every 60 seconds to:
     * 1. Capture the current count
     * 2. Save it as lastMinuteEventCount
     * 3. Reset the counter to 0
     */
    @Scheduled(fixedRate = 60000)
    public void resetAndSaveEventCount() {
        lastMinuteEventCount = messageCounter.getAndSet(0);
        log.debug("Message throughput: {} EPM", lastMinuteEventCount);
    }
}
