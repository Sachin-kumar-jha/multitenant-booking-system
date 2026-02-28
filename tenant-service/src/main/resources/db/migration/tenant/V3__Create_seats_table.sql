-- Tenant schema: Seats table
-- V3__Create_seats_table.sql

CREATE TABLE seats (
    id VARCHAR(36) PRIMARY KEY,
    event_id VARCHAR(36) NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    section VARCHAR(50),
    row_number VARCHAR(10),
    seat_number VARCHAR(10) NOT NULL,
    category VARCHAR(50) DEFAULT 'STANDARD',
    price DECIMAL(10,2) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'AVAILABLE',
    locked_by VARCHAR(36),
    locked_until TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(event_id, section, row_number, seat_number)
);

CREATE INDEX idx_seats_event ON seats(event_id);
CREATE INDEX idx_seats_status ON seats(status);
CREATE INDEX idx_seats_category ON seats(category);
CREATE INDEX idx_seats_locked ON seats(locked_until) WHERE locked_until IS NOT NULL;
