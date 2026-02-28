package com.ticket.common.tenant;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

/**
 * Resolves the current tenant identifier for Hibernate multi-tenancy.
 */
@Component
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<String> {
    
    private static final String DEFAULT_TENANT = "public";
    
    @Override
    public String resolveCurrentTenantIdentifier() {
        String tenant = TenantContext.getCurrentTenant();
        return tenant != null ? tenant : DEFAULT_TENANT;
    }
    
    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}
