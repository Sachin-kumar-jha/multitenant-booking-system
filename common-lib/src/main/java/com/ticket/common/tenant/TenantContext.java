package com.ticket.common.tenant;

/**
 * Thread-local storage for current tenant context.
 * Used throughout the request lifecycle to maintain tenant isolation.
 */
public final class TenantContext {
    
    private static final ThreadLocal<String> CURRENT_TENANT = new InheritableThreadLocal<>();
    private static final ThreadLocal<String> CORRELATION_ID = new InheritableThreadLocal<>();
    
    private TenantContext() {
        // Utility class
    }
    
    public static void setCurrentTenant(String tenantId) {
        CURRENT_TENANT.set(tenantId);
    }
    
    public static String getCurrentTenant() {
        return CURRENT_TENANT.get();
    }
    
    public static void setCorrelationId(String correlationId) {
        CORRELATION_ID.set(correlationId);
    }
    
    public static String getCorrelationId() {
        return CORRELATION_ID.get();
    }
    
    public static void clear() {
        CURRENT_TENANT.remove();
        CORRELATION_ID.remove();
    }
    
    public static String requireCurrentTenant() {
        String tenant = CURRENT_TENANT.get();
        if (tenant == null || tenant.isBlank()) {
            throw new IllegalStateException("No tenant set in current context");
        }
        return tenant;
    }
    
    /**
     * Alias for getCurrentTenant() for compatibility.
     */
    public static String getTenantId() {
        return getCurrentTenant();
    }
}
