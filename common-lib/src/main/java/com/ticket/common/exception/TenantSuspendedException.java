package com.ticket.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a tenant is suspended.
 */
public class TenantSuspendedException extends BaseException {
    
    public TenantSuspendedException(String tenantId) {
        super("Tenant is suspended: " + tenantId, HttpStatus.FORBIDDEN, "TENANT_SUSPENDED");
    }
}
