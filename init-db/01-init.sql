-- Initialize database with required extensions and schemas
-- This script runs when PostgreSQL container starts for the first time

-- Create extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- Create platform schema (for future platform-level tables if needed)
CREATE SCHEMA IF NOT EXISTS platform;

-- Create default tenant schema (for development)
CREATE SCHEMA IF NOT EXISTS tenant_default;

-- Grant permissions
GRANT ALL ON SCHEMA platform TO admin;
GRANT ALL ON SCHEMA tenant_default TO admin;

-- Log completion
DO $$
BEGIN
    RAISE NOTICE 'Database initialization completed successfully';
END $$;
