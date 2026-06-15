package com.acousticguard.hub.common.constant;

public final class WebSocketTopics {
    public static final String ALERTS = "/topic/alerts";
    public static final String INCIDENTS = "/topic/incidents";
    public static final String TELEMETRY = "/topic/telemetry";
    public static final String SENSORS = "/topic/sensors";
    public static final String TOPIC_PREFIX = "/topic";
    public static final String APP_PREFIX = "/app";
    public static final String WS_ENDPOINT = "/ws";

    private WebSocketTopics() {
    }
}
