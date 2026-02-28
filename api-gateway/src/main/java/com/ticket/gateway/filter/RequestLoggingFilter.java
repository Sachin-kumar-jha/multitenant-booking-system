package com.ticket.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        ServerHttpRequest request = exchange.getRequest();
        
        long startTime = System.currentTimeMillis();
        
        logger.info("[{}] {} {} - Tenant: {}",
            requestId,
            request.getMethod(),
            request.getPath(),
            request.getHeaders().getFirst("X-Tenant-Id")
        );

        // Add request ID header
        ServerHttpRequest modifiedRequest = request.mutate()
            .header("X-Request-Id", requestId)
            .build();

        return chain.filter(exchange.mutate().request(modifiedRequest).build())
            .then(Mono.fromRunnable(() -> {
                long duration = System.currentTimeMillis() - startTime;
                logger.info("[{}] Response: {} - {}ms",
                    requestId,
                    exchange.getResponse().getStatusCode(),
                    duration
                );
            }));
    }

    @Override
    public int getOrder() {
        return -200; // Execute before authentication filter
    }
}
