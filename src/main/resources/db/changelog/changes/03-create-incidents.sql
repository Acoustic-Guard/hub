-- liquibase formatted sql

-- changeset humaNukr:create-incidents
CREATE TABLE incidents
(
    id            UUID PRIMARY KEY         DEFAULT gen_random_uuid(),
    location_geo  GEOGRAPHY(Point, 4326)  NOT NULL,
    type          VARCHAR(50)             NOT NULL,
    intensity     REAL                    NOT NULL,
    status        VARCHAR(50)             NOT NULL,
    sensor_id     VARCHAR(50),
    created_at    TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    metadata      JSONB
);

-- Spatial index for location queries
CREATE INDEX idx_incidents_location_geo ON incidents USING GIST(location_geo);

-- rollback DROP TABLE incidents;