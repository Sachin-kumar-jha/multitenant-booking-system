package com.ticket.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final SecretKey secretKey;
    
    // Paths that don't require authentication
    private static final List<String> PUBLIC_PATHS = List.of(
        "/api/auth/login",
        "/api/auth/register",
        "/api/auth/refresh",
        "/api/tenants/register",
        "/api/tenants/validate",
        "/api/events",           // Public event listing
        "/actuator"
    );

    public JwtAuthenticationFilter(@Value("${jwt.secret.key}") String secretKeyString) {
        this.secretKey = Keys.hmacShaKeyFor(secretKeyString.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // Skip authentication for public paths
        if (isPublicPath(path)) {
            logger.debug("Skipping authentication for public path: {}", path);
            return chain.filter(exchange);
        }

        // Check for Authorization header
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warn("Missing or invalid Authorization header for path: {}", path);
            return unauthorizedResponse(exchange, "Missing or invalid authorization header");
        }

        String token = authHeader.substring(7);
        
        try {
            Claims claims = validateToken(token);
            
            // Extract tenant from token and add to request headers
            String tenantId = claims.get("tenantId", String.class);
            Long userId = claims.get("userId", Long.class);
            String role = claims.get("role", String.class);

            if (tenantId == null) {
                logger.warn("Token missing tenantId claim");
                return unauthorizedResponse(exchange, "Invalid token: missing tenant information");
            }

            // Modify request to add tenant and user info headers for downstream services
            ServerHttpRequest modifiedRequest = request.mutate()
                .header("X-Tenant-Id", tenantId)
                .header("X-User-Id", userId != null ? userId.toString() : "")
                .header("X-User-Role", role != null ? role : "")
                .build();

            logger.debug("Authenticated request for tenant: {}, user: {}", tenantId, userId);
            
            return chain.filter(exchange.mutate().request(modifiedRequest).build());
            
        } catch (ExpiredJwtException e) {
            logger.warn("Expired JWT token for path: {}", path);
            return unauthorizedResponse(exchange, "Token has expired");
        } catch (MalformedJwtException e) {
            logger.warn("Malformed JWT token for path: {}", path);
            return unauthorizedResponse(exchange, "Invalid token format");
        } catch (SignatureException e) {
            logger.warn("Invalid JWT signature for path: {}", path);
            return unauthorizedResponse(exchange, "Invalid token signature");
        } catch (Exception e) {
            logger.error("JWT validation error for path: {}: {}", path, e.getMessage());
            return unauthorizedResponse(exchange, "Token validation failed");
        }
    }

    private Claims validateToken(String token) {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(publicPath -> {
            if (publicPath.endsWith("/**")) {
                return path.startsWith(publicPath.substring(0, publicPath.length() - 3));
            } else if (publicPath.equals("/api/events")) {
                // Allow GET requests to /api/events and /api/events/* 
                return path.startsWith("/api/events");
            }
            return path.startsWith(publicPath);
        });
    }

    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json");
        
        String body = String.format("{\"error\":\"Unauthorized\",\"message\":\"%s\"}", message);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }

    @Override
    public int getOrder() {
        return -100; // Execute early in the filter chain
    }
}
