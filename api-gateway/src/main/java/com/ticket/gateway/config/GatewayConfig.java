package com.ticket.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            // Health check aggregator
            .route("health-check", r -> r
                .path("/health")
                .uri("http://localhost:8080/actuator/health"))
            
            // Fallback route for circuit breaker
            .route("fallback", r -> r
                .path("/fallback")
                .filters(f -> f.setStatus(HttpStatus.SERVICE_UNAVAILABLE))
                .uri("no://op"))
            
            .build();
    }
}
