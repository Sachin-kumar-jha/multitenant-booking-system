-- Tenant schema: Processed events for idempotency
-- V6__Create_processed_events_table.sql

CREATE TABLE processed_events (
    id VARCHAR(36) PRIMARY KEY,
    event_id VARCHAR(255) NOT NULL,
    consumer_group VARCHAR(100) NOT NULL,
    event_type VARCHAR(100),
    processed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(event_id, consumer_group)
);

CREATE INDEX idx_processed_events_lookup ON processed_events(event_id, consumer_group);
