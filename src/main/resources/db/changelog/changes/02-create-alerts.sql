-- liquibase formatted sql

-- changeset humaNukr:create-alerts
CREATE TABLE alerts
(
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    threat_type VARCHAR(50)              NOT NULL,
    confidence  REAL                     NOT NULL,
    location    VARCHAR(255)             NOT NULL,
    detected_at TIMESTAMP WITH TIME ZONE NOT NULL,
    sensor_id   VARCHAR(50),
    location_geo GEOGRAPHY(Point, 4326) NOT NULL,
    metadata    JSONB
);

-- Index for idempotency check (sensor_id + detected_at)
CREATE INDEX idx_alerts_sensor_detected ON alerts(sensor_id, detected_at);

-- Spatial index for location queries
CREATE INDEX idx_alerts_location_geo ON alerts USING GIST(location_geo);

-- rollback DROP TABLE alerts;