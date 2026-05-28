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
    latitude    REAL,
    longitude   REAL,
    metadata    JSONB
);
-- rollback DROP TABLE alerts;