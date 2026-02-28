package com.ticket.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Base class for all domain events.
 * Provides common fields for event sourcing and tracing.
 */
public abstract class BaseEvent {
    
    private String eventId;
    private String tenantId;
    private String correlationId;
    private Instant timestamp;
    private String eventType;
    private int version;
    
    protected BaseEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.timestamp = Instant.now();
        this.version = 1;
        this.eventType = this.getClass().getSimpleName();
    }
    
    protected BaseEvent(String tenantId, String correlationId) {
        this();
        this.tenantId = tenantId;
        this.correlationId = correlationId;
    }
    
    // Getters and Setters
    public String getEventId() {
        return eventId;
    }
    
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }
    
    public String getTenantId() {
        return tenantId;
    }
    
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
    
    public String getCorrelationId() {
        return correlationId;
    }
    
    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getEventType() {
        return eventType;
    }
    
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
    
    public int getVersion() {
        return version;
    }
    
    public void setVersion(int version) {
        this.version = version;
    }
}
