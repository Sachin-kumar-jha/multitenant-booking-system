package com.ticket.common.event;

import java.math.BigDecimal;

/**
 * Event published when payment fails.
 */
public class PaymentFailedEvent extends BaseEvent {
    
    private String paymentId;
    private String bookingId;
    private BigDecimal amount;
    private String failureReason;
    private String errorCode;
    
    public PaymentFailedEvent() {
        super();
    }
    
    public PaymentFailedEvent(String tenantId, String correlationId, String paymentId,
                               String bookingId, BigDecimal amount, String failureReason,
                               String errorCode) {
        super(tenantId, correlationId);
        this.paymentId = paymentId;
        this.bookingId = bookingId;
        this.amount = amount;
        this.failureReason = failureReason;
        this.errorCode = errorCode;
    }
    
    public String getPaymentId() {
        return paymentId;
    }
    
    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }
    
    public String getBookingId() {
        return bookingId;
    }
    
    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public String getFailureReason() {
        return failureReason;
    }
    
    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
}
