--liquibase formatted sql

--changeset developer:2
CREATE TABLE categories (
    id            BIGSERIAL    PRIMARY KEY,
    name          VARCHAR(100) NOT NULL UNIQUE,
    description   TEXT,
    created_by_id BIGINT REFERENCES users(id) ON DELETE SET NULL
);
