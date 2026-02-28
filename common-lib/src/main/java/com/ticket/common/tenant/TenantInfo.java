package com.ticket.common.tenant;

/**
 * Tenant information for caching and validation.
 */
public class TenantInfo {
    
    private String id;
    private String name;
    private String schemaName;
    private String status;
    private String plan;
    
    public TenantInfo() {}
    
    public TenantInfo(String id, String name, String schemaName, String status, String plan) {
        this.id = id;
        this.name = name;
        this.schemaName = schemaName;
        this.status = status;
        this.plan = plan;
    }
    
    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(status);
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getSchemaName() {
        return schemaName;
    }
    
    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getPlan() {
        return plan;
    }
    
    public void setPlan(String plan) {
        this.plan = plan;
    }
}
