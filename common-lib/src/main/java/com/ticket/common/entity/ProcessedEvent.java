package com.ticket.common.entity;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Tracks processed events for idempotent consumers.
 */
@Entity
@Table(name = "processed_events", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "consumer_group"}))
public class ProcessedEvent {
    
    @Id
    @Column(length = 36)
    private String id;
    
    @Column(name = "event_id", nullable = false)
    private String eventId;
    
    @Column(name = "consumer_group", nullable = false, length = 100)
    private String consumerGroup;
    
    @Column(name = "event_type", length = 100)
    private String eventType;
    
    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;
    
    public ProcessedEvent() {
        this.processedAt = Instant.now();
    }
    
    public ProcessedEvent(String id, String eventId, String consumerGroup, String eventType) {
        this();
        this.id = id;
        this.eventId = eventId;
        this.consumerGroup = consumerGroup;
        this.eventType = eventType;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getEventId() {
        return eventId;
    }
    
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }
    
    public String getConsumerGroup() {
        return consumerGroup;
    }
    
    public void setConsumerGroup(String consumerGroup) {
        this.consumerGroup = consumerGroup;
    }
    
    public String getEventType() {
        return eventType;
    }
    
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
    
    public Instant getProcessedAt() {
        return processedAt;
    }
    
    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }
}
