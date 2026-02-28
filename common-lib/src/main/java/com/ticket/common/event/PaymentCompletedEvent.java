package com.ticket.common.event;

import java.math.BigDecimal;

/**
 * Event published when payment is successfully completed.
 */
public class PaymentCompletedEvent extends BaseEvent {
    
    private String paymentId;
    private String bookingId;
    private BigDecimal amount;
    private String transactionRef;
    private String paymentMethod;
    
    public PaymentCompletedEvent() {
        super();
    }
    
    public PaymentCompletedEvent(String tenantId, String correlationId, String paymentId,
                                  String bookingId, BigDecimal amount, String transactionRef,
                                  String paymentMethod) {
        super(tenantId, correlationId);
        this.paymentId = paymentId;
        this.bookingId = bookingId;
        this.amount = amount;
        this.transactionRef = transactionRef;
        this.paymentMethod = paymentMethod;
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
    
    public String getTransactionRef() {
        return transactionRef;
    }
    
    public void setTransactionRef(String transactionRef) {
        this.transactionRef = transactionRef;
    }
    
    public String getPaymentMethod() {
        return paymentMethod;
    }
    
    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}
