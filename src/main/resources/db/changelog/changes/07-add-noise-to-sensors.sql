-- liquibase formatted sql

-- changeset humaNukr:add-noise-to-sensors
ALTER TABLE sensors
ADD COLUMN current_avg_db REAL;

ALTER TABLE sensors
ADD COLUMN noise_updated_at TIMESTAMP WITH TIME ZONE;

-- rollback ALTER TABLE sensors DROP COLUMN current_avg_db;
-- rollback ALTER TABLE sensors DROP COLUMN noise_updated_at;
