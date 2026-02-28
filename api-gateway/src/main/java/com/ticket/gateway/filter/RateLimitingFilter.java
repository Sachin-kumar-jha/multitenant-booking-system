package com.ticket.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class RateLimitingFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingFilter.class);

    private final ReactiveStringRedisTemplate redisTemplate;
    
    // Rate limit configuration
    private static final int DEFAULT_REQUESTS_PER_MINUTE = 60;
    private static final int BOOKING_REQUESTS_PER_MINUTE = 20;
    private static final Duration WINDOW_DURATION = Duration.ofMinutes(1);

    @Autowired
    public RateLimitingFilter(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        
        // Get client identifier (prefer tenant + user, fallback to IP)
        String tenantId = request.getHeaders().getFirst("X-Tenant-Id");
        String userId = request.getHeaders().getFirst("X-User-Id");
        String clientIp = getClientIp(request);
        
        String clientKey = buildClientKey(tenantId, userId, clientIp);
        int limit = getLimit(path);
        String rateLimitKey = String.format("rate_limit:%s:%s", path.split("/")[2], clientKey);

        return checkRateLimit(rateLimitKey, limit)
            .flatMap(allowed -> {
                if (!allowed) {
                    logger.warn("Rate limit exceeded for client: {} on path: {}", clientKey, path);
                    return rateLimitExceededResponse(exchange);
                }
                return chain.filter(exchange);
            });
    }

    private Mono<Boolean> checkRateLimit(String key, int limit) {
        return redisTemplate.opsForValue().increment(key)
            .flatMap(count -> {
                if (count == 1L) {
                    // First request - set expiration
                    return redisTemplate.expire(key, WINDOW_DURATION)
                        .thenReturn(true);
                }
                return Mono.just(count <= limit);
            })
            .onErrorReturn(true); // Allow on Redis failure
    }

    private String buildClientKey(String tenantId, String userId, String clientIp) {
        if (tenantId != null && userId != null && !userId.isEmpty()) {
            return tenantId + ":" + userId;
        } else if (tenantId != null) {
            return tenantId + ":" + clientIp;
        }
        return clientIp;
    }

    private String getClientIp(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddress() != null 
            ? request.getRemoteAddress().getAddress().getHostAddress() 
            : "unknown";
    }

    private int getLimit(String path) {
        if (path.startsWith("/api/bookings")) {
            return BOOKING_REQUESTS_PER_MINUTE; // Stricter limit for bookings
        }
        return DEFAULT_REQUESTS_PER_MINUTE;
    }

    private Mono<Void> rateLimitExceededResponse(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().add("Content-Type", "application/json");
        response.getHeaders().add("Retry-After", "60");
        
        String body = "{\"error\":\"Rate limit exceeded\",\"message\":\"Too many requests. Please try again later.\"}";
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }

    @Override
    public int getOrder() {
        return -50; // Execute after authentication
    }
}
