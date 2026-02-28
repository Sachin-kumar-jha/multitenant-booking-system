package com.ticket.common.event;

import java.math.BigDecimal;

/**
 * Event published when a booking is confirmed.
 */
public class BookingConfirmedEvent extends BaseEvent {
    
    private String bookingId;
    private String userId;
    private String eventId;
    private String eventName;
    private String seatId;
    private String seatInfo;
    private BigDecimal totalAmount;
    private String confirmationCode;
    
    public BookingConfirmedEvent() {
        super();
    }
    
    public BookingConfirmedEvent(String tenantId, String correlationId, String bookingId,
                                  String userId, String eventId, String eventName,
                                  String seatId, String seatInfo, BigDecimal totalAmount,
                                  String confirmationCode) {
        super(tenantId, correlationId);
        this.bookingId = bookingId;
        this.userId = userId;
        this.eventId = eventId;
        this.eventName = eventName;
        this.seatId = seatId;
        this.seatInfo = seatInfo;
        this.totalAmount = totalAmount;
        this.confirmationCode = confirmationCode;
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
    
    public String getEventName() {
        return eventName;
    }
    
    public void setEventName(String eventName) {
        this.eventName = eventName;
    }
    
    public String getSeatId() {
        return seatId;
    }
    
    public void setSeatId(String seatId) {
        this.seatId = seatId;
    }
    
    public String getSeatInfo() {
        return seatInfo;
    }
    
    public void setSeatInfo(String seatInfo) {
        this.seatInfo = seatInfo;
    }
    
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
    
    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
    
    public String getConfirmationCode() {
        return confirmationCode;
    }
    
    public void setConfirmationCode(String confirmationCode) {
        this.confirmationCode = confirmationCode;
    }
}
