-- liquibase formatted sql

-- changeset humaNukr:01-create-user
CREATE TABLE auth_users
(
    username VARCHAR(50) PRIMARY KEY,
    password VARCHAR(255) NOT NULL,
    role     VARCHAR(20)  NOT NULL
);
-- rollback DROP TABLE auth_users;