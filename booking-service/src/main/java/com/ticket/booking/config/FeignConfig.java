package com.ticket.booking.config;

import feign.RequestInterceptor;
import com.ticket.common.tenant.TenantContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor tenantHeaderInterceptor() {
        return requestTemplate -> {
            String tenantId = TenantContext.getTenantId();
            if (tenantId != null) {
                requestTemplate.header("X-Tenant-ID", tenantId);
            }
        };
    }
}
