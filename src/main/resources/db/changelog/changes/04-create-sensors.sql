-- liquibase formatted sql

-- changeset humaNukr:create-sensors
CREATE TABLE sensors
(
    id             VARCHAR(50) PRIMARY KEY,
    location       VARCHAR(100) NOT NULL,
    latitude       REAL         NOT NULL,
    longitude      REAL         NOT NULL,
    status         VARCHAR(20)  NOT NULL,
    last_heartbeat TIMESTAMP WITH TIME ZONE,
    created_at     TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
-- rollback DROP TABLE sensors;
