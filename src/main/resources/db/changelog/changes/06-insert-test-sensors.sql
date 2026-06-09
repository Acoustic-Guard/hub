-- liquibase formatted sql

-- changeset humaNukr:insert-test-sensors
INSERT INTO sensors (id, location, latitude, longitude, status)
VALUES ('AG-NODE-001', 'Shevchenkivskyi', 50.4501, 30.5234, 'ONLINE');

INSERT INTO sensors (id, location, latitude, longitude, status)
VALUES ('AG-NODE-002', 'Pecherskyi', 50.4333, 30.5167, 'OFFLINE');

INSERT INTO sensors (id, location, latitude, longitude, status)
VALUES ('AG-NODE-003', 'Podilskyi', 50.4667, 30.5167, 'WARNING');

-- rollback DELETE FROM sensors WHERE id IN ('AG-NODE-001', 'AG-NODE-002', 'AG-NODE-003');
