package com.ticket.common.event;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Event published when a new event is created.
 */
public class EventCreatedEvent extends BaseEvent {
    
    private String eventEntityId;
    private String name;
    private String venue;
    private Instant eventDate;
    private int totalSeats;
    private BigDecimal pricePerSeat;
    private String createdBy;
    
    public EventCreatedEvent() {
        super();
    }
    
    public EventCreatedEvent(String tenantId, String correlationId, String eventEntityId,
                              String name, String venue, Instant eventDate,
                              int totalSeats, BigDecimal pricePerSeat, String createdBy) {
        super(tenantId, correlationId);
        this.eventEntityId = eventEntityId;
        this.name = name;
        this.venue = venue;
        this.eventDate = eventDate;
        this.totalSeats = totalSeats;
        this.pricePerSeat = pricePerSeat;
        this.createdBy = createdBy;
    }
    
    public String getEventEntityId() {
        return eventEntityId;
    }
    
    public void setEventEntityId(String eventEntityId) {
        this.eventEntityId = eventEntityId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getVenue() {
        return venue;
    }
    
    public void setVenue(String venue) {
        this.venue = venue;
    }
    
    public Instant getEventDate() {
        return eventDate;
    }
    
    public void setEventDate(Instant eventDate) {
        this.eventDate = eventDate;
    }
    
    public int getTotalSeats() {
        return totalSeats;
    }
    
    public void setTotalSeats(int totalSeats) {
        this.totalSeats = totalSeats;
    }
    
    public BigDecimal getPricePerSeat() {
        return pricePerSeat;
    }
    
    public void setPricePerSeat(BigDecimal pricePerSeat) {
        this.pricePerSeat = pricePerSeat;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
