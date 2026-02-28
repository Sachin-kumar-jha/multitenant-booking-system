package com.ticket.common.security;

import java.security.Principal;

/**
 * User principal containing authenticated user information.
 */
public class UserPrincipal implements Principal {
    
    private final String userId;
    private final String tenantId;
    private final String role;
    
    public UserPrincipal(String userId, String tenantId, String role) {
        this.userId = userId;
        this.tenantId = tenantId;
        this.role = role;
    }
    
    @Override
    public String getName() {
        return userId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public String getTenantId() {
        return tenantId;
    }
    
    public String getRole() {
        return role;
    }
    
    /**
     * Alias for getUserId() for compatibility.
     */
    public String getId() {
        return userId;
    }
    
    /**
     * Alias for getName() for compatibility.
     */
    public String getUsername() {
        return userId;
    }
    
    /**
     * Get user email (alias for userId).
     */
    public String getEmail() {
        return userId;
    }
}
