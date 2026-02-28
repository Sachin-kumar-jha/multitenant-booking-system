-- Tenant schema: Bookings table
-- V4__Create_bookings_table.sql

CREATE TABLE bookings (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id),
    event_id VARCHAR(36) NOT NULL REFERENCES events(id),
    seat_id VARCHAR(36) NOT NULL REFERENCES seats(id),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    idempotency_key VARCHAR(255) UNIQUE,
    confirmation_code VARCHAR(20) UNIQUE,
    total_amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',
    booking_date TIMESTAMP NOT NULL DEFAULT NOW(),
    confirmed_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    cancellation_reason TEXT,
    expires_at TIMESTAMP,
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_bookings_user ON bookings(user_id);
CREATE INDEX idx_bookings_event ON bookings(event_id);
CREATE INDEX idx_bookings_seat ON bookings(seat_id);
CREATE INDEX idx_bookings_status ON bookings(status);
CREATE INDEX idx_bookings_confirmation ON bookings(confirmation_code);
CREATE INDEX idx_bookings_idempotency ON bookings(idempotency_key);
CREATE INDEX idx_bookings_expires ON bookings(expires_at) WHERE status = 'PENDING';

-- Constraint to prevent duplicate confirmed bookings for same seat
CREATE UNIQUE INDEX idx_bookings_seat_confirmed 
ON bookings(event_id, seat_id) 
WHERE status IN ('CONFIRMED', 'PENDING');
