package com.ticket.common.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * Distributed lock service using Redis.
 * Used for seat locking with TTL.
 */
@Service
public class RedisLockService {
    
    private static final Logger log = LoggerFactory.getLogger(RedisLockService.class);
    
    private static final String SEAT_LOCK_PREFIX = "seat:lock:";
    private static final String DISTRIBUTED_LOCK_PREFIX = "dlock:";
    
    // Lua script for atomic unlock - only unlocks if value matches
    private static final String UNLOCK_SCRIPT = 
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "    return redis.call('del', KEYS[1]) " +
            "else " +
            "    return 0 " +
            "end";
    
    private final StringRedisTemplate redisTemplate;
    
    public RedisLockService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    /**
     * Attempt to lock a seat for a user.
     * 
     * @param tenantId Tenant identifier
     * @param seatId Seat identifier
     * @param userId User attempting to lock
     * @param ttl Lock timeout duration
     * @return true if lock acquired, false if seat already locked
     */
    public boolean lockSeat(String tenantId, String seatId, String userId, Duration ttl) {
        String key = buildSeatLockKey(tenantId, seatId);
        String value = buildLockValue(userId);
        
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(key, value, ttl);
        
        if (Boolean.TRUE.equals(acquired)) {
            log.info("Seat lock acquired: tenant={}, seat={}, user={}, ttl={}s", 
                    tenantId, seatId, userId, ttl.getSeconds());
            return true;
        }
        
        log.debug("Seat lock failed (already locked): tenant={}, seat={}", tenantId, seatId);
        return false;
    }
    
    /**
     * Release a seat lock. Only the user who locked it can release.
     */
    public boolean releaseSeatLock(String tenantId, String seatId, String userId) {
        String key = buildSeatLockKey(tenantId, seatId);
        String expectedValue = buildLockValue(userId);
        
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class);
        Long result = redisTemplate.execute(script, 
                Collections.singletonList(key), expectedValue);
        
        boolean released = result != null && result > 0;
        if (released) {
            log.info("Seat lock released: tenant={}, seat={}, user={}", tenantId, seatId, userId);
        }
        
        return released;
    }
    
    /**
     * Check if a seat is locked.
     */
    public boolean isSeatLocked(String tenantId, String seatId) {
        String key = buildSeatLockKey(tenantId, seatId);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
    
    /**
     * Get the user who holds the seat lock.
     */
    public String getSeatLockHolder(String tenantId, String seatId) {
        String key = buildSeatLockKey(tenantId, seatId);
        String value = redisTemplate.opsForValue().get(key);
        
        if (value != null && value.startsWith("user:")) {
            return value.substring(5);
        }
        return null;
    }
    
    /**
     * Get remaining TTL for a seat lock.
     */
    public long getSeatLockTTL(String tenantId, String seatId) {
        String key = buildSeatLockKey(tenantId, seatId);
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return ttl != null ? ttl : -2;
    }
    
    /**
     * Force release a seat lock (admin operation).
     */
    public void forceReleaseSeatLock(String tenantId, String seatId) {
        String key = buildSeatLockKey(tenantId, seatId);
        redisTemplate.delete(key);
        log.warn("Seat lock force released: tenant={}, seat={}", tenantId, seatId);
    }
    
    /**
     * Acquire a general-purpose distributed lock.
     */
    public boolean acquireLock(String lockName, String lockValue, Duration ttl) {
        String key = DISTRIBUTED_LOCK_PREFIX + lockName;
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, lockValue, ttl);
        return Boolean.TRUE.equals(acquired);
    }
    
    /**
     * Release a general-purpose distributed lock.
     */
    public boolean releaseLock(String lockName, String lockValue) {
        String key = DISTRIBUTED_LOCK_PREFIX + lockName;
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class);
        Long result = redisTemplate.execute(script, Collections.singletonList(key), lockValue);
        return result != null && result > 0;
    }
    
    /**
     * Extend the TTL of an existing seat lock.
     */
    public boolean extendSeatLock(String tenantId, String seatId, String userId, Duration additionalTtl) {
        String key = buildSeatLockKey(tenantId, seatId);
        String currentValue = redisTemplate.opsForValue().get(key);
        String expectedValue = buildLockValue(userId);
        
        if (expectedValue.equals(currentValue)) {
            return Boolean.TRUE.equals(redisTemplate.expire(key, additionalTtl));
        }
        return false;
    }
    
    private String buildSeatLockKey(String tenantId, String seatId) {
        return SEAT_LOCK_PREFIX + tenantId + ":" + seatId;
    }
    
    private String buildLockValue(String userId) {
        return "user:" + userId;
    }
    
    /**
     * Simple lock acquisition for a named lock.
     */
    public boolean tryLock(String lockName, Duration ttl) {
        return acquireLock(lockName, "locked", ttl);
    }
    
    /**
     * Simple unlock for a named lock.
     */
    public void unlock(String lockName) {
        String key = DISTRIBUTED_LOCK_PREFIX + lockName;
        redisTemplate.delete(key);
    }
}
