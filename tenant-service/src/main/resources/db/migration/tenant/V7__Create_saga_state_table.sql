-- Tenant schema: Saga state for distributed transactions
-- V7__Create_saga_state_table.sql

CREATE TABLE saga_state (
    id VARCHAR(36) PRIMARY KEY,
    saga_type VARCHAR(100) NOT NULL,
    correlation_id VARCHAR(255) NOT NULL UNIQUE,
    current_step VARCHAR(100),
    status VARCHAR(50) NOT NULL DEFAULT 'STARTED',
    payload JSONB,
    error_message TEXT,
    retry_count INTEGER DEFAULT 0,
    started_at TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_saga_correlation ON saga_state(correlation_id);
CREATE INDEX idx_saga_status ON saga_state(status);
CREATE INDEX idx_saga_type ON saga_state(saga_type);
