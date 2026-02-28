package com.ticket.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a tenant is not found or invalid.
 */
public class TenantNotFoundException extends BaseException {
    
    public TenantNotFoundException(String tenantId) {
        super("Tenant not found: " + tenantId, HttpStatus.NOT_FOUND, "TENANT_NOT_FOUND");
    }
}
