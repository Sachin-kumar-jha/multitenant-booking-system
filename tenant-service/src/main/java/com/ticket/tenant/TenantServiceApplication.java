package com.ticket.tenant;

import com.ticket.common.tenant.TenantFilter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

import java.util.TimeZone;

@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan(
    basePackages = {"com.ticket.tenant", "com.ticket.common"},
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = TenantFilter.class)
)
public class TenantServiceApplication {

    static {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    public static void main(String[] args) {
        SpringApplication.run(TenantServiceApplication.class, args);
    }
}
