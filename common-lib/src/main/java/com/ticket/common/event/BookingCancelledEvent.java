package com.ticket.common.event;

/**
 * Event published when a booking is cancelled.
 */
public class BookingCancelledEvent extends BaseEvent {
    
    private String bookingId;
    private String userId;
    private String eventId;
    private String seatId;
    private String cancellationReason;
    private boolean refundRequired;
    
    public BookingCancelledEvent() {
        super();
    }
    
    public BookingCancelledEvent(String tenantId, String correlationId, String bookingId,
                                  String userId, String eventId, String seatId,
                                  String cancellationReason, boolean refundRequired) {
        super(tenantId, correlationId);
        this.bookingId = bookingId;
        this.userId = userId;
        this.eventId = eventId;
        this.seatId = seatId;
        this.cancellationReason = cancellationReason;
        this.refundRequired = refundRequired;
    }
    
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
    
    public String getCancellationReason() {
        return cancellationReason;
    }
    
    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }
    
    public boolean isRefundRequired() {
        return refundRequired;
    }
    
    public void setRefundRequired(boolean refundRequired) {
        this.refundRequired = refundRequired;
    }
}
