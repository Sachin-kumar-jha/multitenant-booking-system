package com.ticket.tenant.config;

import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;

import java.util.Map;

@Configuration
public class MultiTenantJpaConfig {

    private final MultiTenantConnectionProvider connectionProvider;
    private final CurrentTenantIdentifierResolver tenantResolver;

    public MultiTenantJpaConfig(
            MultiTenantConnectionProvider connectionProvider,
            CurrentTenantIdentifierResolver tenantResolver
    ) {
        this.connectionProvider = connectionProvider;
        this.tenantResolver = tenantResolver;
    }

    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer() {

        return new HibernatePropertiesCustomizer() {

            @Override
            public void customize(Map<String, Object> hibernateProps) {

                hibernateProps.put(
                        "hibernate.multi_tenant_connection_provider",
                        connectionProvider
                );

                hibernateProps.put(
                        "hibernate.tenant_identifier_resolver",
                        tenantResolver
                );

            }

        };

    }

}
