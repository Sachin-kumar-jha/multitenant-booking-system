package com.ticket.common.tenant;

/**
 * Interface for tenant registry operations.
 * Implementations can use database, Redis cache, or both.
 */
public interface TenantRegistry {
    
    /**
     * Get tenant information by tenant identifier (subdomain or ID).
     */
    TenantInfo getTenantInfo(String tenantId);
    
    /**
     * Register a new tenant.
     */
    void registerTenant(TenantInfo tenantInfo);
    
    /**
     * Update tenant status.
     */
    void updateTenantStatus(String tenantId, String status);
    
    /**
     * Invalidate cached tenant info.
     */
    void invalidateCache(String tenantId);
    
    /**
     * Check if tenant exists.
     */
    boolean tenantExists(String tenantId);
}
