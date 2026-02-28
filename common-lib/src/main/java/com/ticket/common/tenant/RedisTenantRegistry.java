package com.ticket.common.tenant;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis-based tenant registry with caching.
 */
@Service
public class RedisTenantRegistry implements TenantRegistry {
    
    private static final Logger log = LoggerFactory.getLogger(RedisTenantRegistry.class);
    private static final String TENANT_KEY_PREFIX = "tenant:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);
    
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    
    public RedisTenantRegistry(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public TenantInfo getTenantInfo(String tenantId) {
        try {
            String key = TENANT_KEY_PREFIX + tenantId;
            String json = redisTemplate.opsForValue().get(key);
            
            if (json != null) {
                return objectMapper.readValue(json, TenantInfo.class);
            }
            
            // If not in cache, also try by schema name
            String schemaKey = TENANT_KEY_PREFIX + "schema:" + tenantId;
            json = redisTemplate.opsForValue().get(schemaKey);
            if (json != null) {
                return objectMapper.readValue(json, TenantInfo.class);
            }
            
        } catch (Exception e) {
            log.error("Error retrieving tenant info from Redis: {}", e.getMessage());
        }
        
        return null;
    }
    
    @Override
    public void registerTenant(TenantInfo tenantInfo) {
        try {
            String json = objectMapper.writeValueAsString(tenantInfo);
            
            // Store by ID
            String idKey = TENANT_KEY_PREFIX + tenantInfo.getId();
            redisTemplate.opsForValue().set(idKey, json, CACHE_TTL);
            
            // Store by schema name for lookup
            String schemaKey = TENANT_KEY_PREFIX + "schema:" + tenantInfo.getSchemaName();
            redisTemplate.opsForValue().set(schemaKey, json, CACHE_TTL);
            
            // Store by name (subdomain) for lookup
            String nameKey = TENANT_KEY_PREFIX + tenantInfo.getName().toLowerCase();
            redisTemplate.opsForValue().set(nameKey, json, CACHE_TTL);
            
            log.info("Registered tenant in cache: {}", tenantInfo.getId());
            
        } catch (Exception e) {
            log.error("Error registering tenant in Redis: {}", e.getMessage());
        }
    }
    
    @Override
    public void updateTenantStatus(String tenantId, String status) {
        TenantInfo info = getTenantInfo(tenantId);
        if (info != null) {
            info.setStatus(status);
            registerTenant(info);
        }
    }
    
    @Override
    public void invalidateCache(String tenantId) {
        try {
            TenantInfo info = getTenantInfo(tenantId);
            if (info != null) {
                redisTemplate.delete(TENANT_KEY_PREFIX + info.getId());
                redisTemplate.delete(TENANT_KEY_PREFIX + "schema:" + info.getSchemaName());
                redisTemplate.delete(TENANT_KEY_PREFIX + info.getName().toLowerCase());
            }
            
            log.info("Invalidated cache for tenant: {}", tenantId);
            
        } catch (Exception e) {
            log.error("Error invalidating tenant cache: {}", e.getMessage());
        }
    }
    
    @Override
    public boolean tenantExists(String tenantId) {
        return getTenantInfo(tenantId) != null;
    }
}
