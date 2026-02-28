package com.ticket.common.config;

import com.ticket.common.tenant.SchemaBasedMultiTenantConnectionProvider;
import com.ticket.common.tenant.TenantIdentifierResolver;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Hibernate multitenancy configuration.
 */
@Configuration
public class HibernateMultiTenancyConfig {
    
    private final SchemaBasedMultiTenantConnectionProvider connectionProvider;
    private final TenantIdentifierResolver tenantResolver;
    
    public HibernateMultiTenancyConfig(
            SchemaBasedMultiTenantConnectionProvider connectionProvider,
            TenantIdentifierResolver tenantResolver) {
        this.connectionProvider = connectionProvider;
        this.tenantResolver = tenantResolver;
    }
    
    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer() {
        return (Map<String, Object> hibernateProperties) -> {
            hibernateProperties.put(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER, connectionProvider);
            hibernateProperties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, tenantResolver);
        };
    }
}
