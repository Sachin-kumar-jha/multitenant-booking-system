-- Initialize database with required extensions and platform schema
-- This script runs when PostgreSQL container starts for the first time

-- Create extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- Create platform schema for tenant management
CREATE SCHEMA IF NOT EXISTS platform;

-- Platform tenant registry table
CREATE TABLE IF NOT EXISTS platform.tenants (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    schema_name VARCHAR(63) NOT NULL,
    database_url VARCHAR(500),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    subscription_plan VARCHAR(50) DEFAULT 'BASIC',
    admin_email VARCHAR(255),
    settings JSONB DEFAULT '{}',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create index on tenant_id
CREATE INDEX IF NOT EXISTS idx_tenants_tenant_id ON platform.tenants(tenant_id);
CREATE INDEX IF NOT EXISTS idx_tenants_status ON platform.tenants(status);

-- Function to update timestamp
CREATE OR REPLACE FUNCTION platform.update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger for updated_at
DROP TRIGGER IF EXISTS trigger_tenant_updated_at ON platform.tenants;
CREATE TRIGGER trigger_tenant_updated_at
    BEFORE UPDATE ON platform.tenants
    FOR EACH ROW
    EXECUTE FUNCTION platform.update_updated_at();

-- Grant permissions
GRANT ALL ON SCHEMA platform TO admin;
GRANT ALL ON ALL TABLES IN SCHEMA platform TO admin;
GRANT ALL ON ALL SEQUENCES IN SCHEMA platform TO admin;

-- Insert a default tenant for development
INSERT INTO platform.tenants (tenant_id, name, schema_name, admin_email, subscription_plan)
VALUES ('default', 'Default Organization', 'tenant_default', 'admin@example.com', 'ENTERPRISE')
ON CONFLICT (tenant_id) DO NOTHING;

-- Create the default tenant schema
CREATE SCHEMA IF NOT EXISTS tenant_default;

-- Log completion
DO $$
BEGIN
    RAISE NOTICE 'Database initialization completed successfully';
END $$;
