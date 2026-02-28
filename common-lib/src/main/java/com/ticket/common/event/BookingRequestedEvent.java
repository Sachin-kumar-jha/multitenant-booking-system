package com.ticket.common.event;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Event published when a booking is requested.
 */
public class BookingRequestedEvent extends BaseEvent {
    
    private String bookingId;
    private String userId;
    private String eventId;
    private String seatId;
    private BigDecimal amount;
    private String idempotencyKey;
    private Instant expiresAt;
    
    public BookingRequestedEvent() {
        super();
    }
    
    public BookingRequestedEvent(String tenantId, String correlationId, String bookingId,
                                  String userId, String eventId, String seatId, 
                                  BigDecimal amount, String idempotencyKey, Instant expiresAt) {
        super(tenantId, correlationId);
        this.bookingId = bookingId;
        this.userId = userId;
        this.eventId = eventId;
        this.seatId = seatId;
        this.amount = amount;
        this.idempotencyKey = idempotencyKey;
        this.expiresAt = expiresAt;
    }
    
    // Getters and Setters
    public String getBookingId() {
        return bookingId;
    }
    
    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getEventId() {
        return eventId;
    }
    
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }
    
    public String getSeatId() {
        return seatId;
    }
    
    public void setSeatId(String seatId) {
        this.seatId = seatId;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public String getIdempotencyKey() {
        return idempotencyKey;
    }
    
    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
    
    public Instant getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
