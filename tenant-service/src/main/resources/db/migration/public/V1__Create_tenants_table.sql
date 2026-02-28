-- Public schema: Tenant registry table
-- V1__Create_tenants_table.sql

CREATE TABLE IF NOT EXISTS tenants (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    subdomain VARCHAR(50) NOT NULL UNIQUE,
    schema_name VARCHAR(63) NOT NULL UNIQUE,
    admin_email VARCHAR(255) NOT NULL,
    plan VARCHAR(50) DEFAULT 'BASIC',
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    max_users INTEGER DEFAULT 100,
    max_events INTEGER DEFAULT 50,
    settings TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_tenant_subdomain ON tenants(subdomain);
CREATE INDEX idx_tenant_status ON tenants(status);
CREATE INDEX idx_tenant_schema ON tenants(schema_name);
