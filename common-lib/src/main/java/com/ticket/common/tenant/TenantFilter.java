package com.ticket.common.tenant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.common.dto.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter to extract tenant from subdomain and set in TenantContext.
 * Must run before JwtAuthenticationFilter.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TenantFilter extends OncePerRequestFilter {
    
    private static final Logger log = LoggerFactory.getLogger(TenantFilter.class);
    private static final String TENANT_HEADER = "X-Tenant-ID";
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    
    private final TenantRegistry tenantRegistry;
    private final ObjectMapper objectMapper;
    
    public TenantFilter(TenantRegistry tenantRegistry, ObjectMapper objectMapper) {
        this.tenantRegistry = tenantRegistry;
        this.objectMapper = objectMapper;
    }
    
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        
        try {
            // Set correlation ID for tracing
            String correlationId = request.getHeader(CORRELATION_ID_HEADER);
            if (correlationId == null || correlationId.isBlank()) {
                correlationId = UUID.randomUUID().toString();
            }
            TenantContext.setCorrelationId(correlationId);
            MDC.put("correlationId", correlationId);
            response.setHeader(CORRELATION_ID_HEADER, correlationId);
            
            // Extract tenant from subdomain or header
            String tenantId = extractTenantId(request);
            
            if (tenantId == null || tenantId.isBlank()) {
                // Allow requests without tenant for certain endpoints
                if (shouldSkipTenantValidation(request)) {
                    filterChain.doFilter(request, response);
                    return;
                }
                
                sendErrorResponse(response, HttpStatus.BAD_REQUEST, 
                        "TENANT_REQUIRED", "Tenant identifier is required");
                return;
            }
            
            // Validate tenant exists and is active
            TenantInfo tenantInfo = tenantRegistry.getTenantInfo(tenantId);
            if (tenantInfo == null) {
                sendErrorResponse(response, HttpStatus.NOT_FOUND, 
                        "TENANT_NOT_FOUND", "Tenant not found: " + tenantId);
                return;
            }
            
            if (!tenantInfo.isActive()) {
                sendErrorResponse(response, HttpStatus.FORBIDDEN, 
                        "TENANT_SUSPENDED", "Tenant is suspended: " + tenantId);
                return;
            }
            
            // Set tenant context
            TenantContext.setCurrentTenant(tenantInfo.getSchemaName());
            MDC.put("tenantId", tenantInfo.getSchemaName());
            
            log.debug("Request for tenant: {} (schema: {})", tenantId, tenantInfo.getSchemaName());
            
            filterChain.doFilter(request, response);
            
        } finally {
            TenantContext.clear();
            MDC.clear();
        }
    }
    
    private String extractTenantId(HttpServletRequest request) {
        // First try header (for service-to-service calls)
        String tenantHeader = request.getHeader(TENANT_HEADER);
        if (tenantHeader != null && !tenantHeader.isBlank()) {
            return tenantHeader;
        }
        
        // Extract from subdomain
        String serverName = request.getServerName();
        if (serverName != null) {
            // Handle patterns like tenant1.localhost or tenant1.yourapp.com
            String[] parts = serverName.split("\\.");
            if (parts.length >= 2) {
                String subdomain = parts[0];
                // Skip common non-tenant subdomains
                if (!subdomain.equals("www") && !subdomain.equals("api") && 
                    !subdomain.equals("localhost")) {
                    return subdomain;
                }
            }
        }
        
        return null;
    }
    
    private boolean shouldSkipTenantValidation(HttpServletRequest request) {
        String path = request.getServletPath();
        String method = request.getMethod();
        
        // Skip tenant validation for:
        // - Actuator endpoints
        // - All tenant management endpoints (tenant-service manages tenants, not per-tenant data)
        // - Public authentication endpoints
        return path.startsWith("/actuator") ||
               path.startsWith("/api/tenants") ||
               path.startsWith("/api/auth/login") ||
               path.startsWith("/api/auth/register") ||
               path.startsWith("/api/auth/refresh");
    }
    
    private void sendErrorResponse(HttpServletResponse response, HttpStatus status,
                                   String errorCode, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        
        ErrorResponse errorResponse = new ErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                errorCode,
                message,
                null
        );
        errorResponse.setCorrelationId(TenantContext.getCorrelationId());
        
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}
