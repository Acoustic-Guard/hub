-- liquibase formatted sql

-- changeset humaNukr:05-insert-admin
INSERT INTO auth_users (username, password, role)
VALUES ('admin', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HCGzBwGZhlz2K6xWmF8uu', 'ADMIN');