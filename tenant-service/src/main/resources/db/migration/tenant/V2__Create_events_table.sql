-- Tenant schema: Events table
-- V2__Create_events_table.sql

CREATE TABLE events (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    venue VARCHAR(255) NOT NULL,
    address TEXT,
    city VARCHAR(100),
    event_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP,
    total_seats INTEGER NOT NULL,
    available_seats INTEGER NOT NULL,
    base_price DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    category VARCHAR(100),
    tags TEXT,
    image_url TEXT,
    created_by VARCHAR(36) NOT NULL REFERENCES users(id),
    published_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    cancellation_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_events_status ON events(status);
CREATE INDEX idx_events_date ON events(event_date);
CREATE INDEX idx_events_created_by ON events(created_by);
CREATE INDEX idx_events_category ON events(category);
