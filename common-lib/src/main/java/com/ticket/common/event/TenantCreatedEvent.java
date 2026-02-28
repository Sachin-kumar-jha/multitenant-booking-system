package com.ticket.common.event;

/**
 * Event published when a new tenant is created/onboarded.
 */
public class TenantCreatedEvent extends BaseEvent {
    
    private String tenantName;
    private String schemaName;
    private String adminEmail;
    private String plan;
    
    public TenantCreatedEvent() {
        super();
    }
    
    public TenantCreatedEvent(String tenantId, String correlationId, String tenantName, 
                               String schemaName, String adminEmail, String plan) {
        super(tenantId, correlationId);
        this.tenantName = tenantName;
        this.schemaName = schemaName;
        this.adminEmail = adminEmail;
        this.plan = plan;
    }
    
    public String getTenantName() {
        return tenantName;
    }
    
    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }
    
    public String getSchemaName() {
        return schemaName;
    }
    
    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }
    
    public String getAdminEmail() {
        return adminEmail;
    }
    
    public void setAdminEmail(String adminEmail) {
        this.adminEmail = adminEmail;
    }
    
    public String getPlan() {
        return plan;
    }
    
    public void setPlan(String plan) {
        this.plan = plan;
    }
}
