package com.ticket.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown for cross-tenant access violations.
 */
public class CrossTenantAccessException extends BaseException {
    
    public CrossTenantAccessException() {
        super("Cross-tenant access is not permitted", HttpStatus.FORBIDDEN, "CROSS_TENANT_ACCESS");
    }
    
    public CrossTenantAccessException(String expectedTenant, String actualTenant) {
        super("Cross-tenant access: expected " + expectedTenant + " but got " + actualTenant,
              HttpStatus.FORBIDDEN, "CROSS_TENANT_ACCESS");
    }
}
