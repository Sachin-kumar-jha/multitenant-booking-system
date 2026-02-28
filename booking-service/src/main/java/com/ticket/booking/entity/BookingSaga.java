package com.ticket.booking.entity;

import com.ticket.common.entity.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "saga_state", indexes = {
    @Index(name = "idx_saga_booking", columnList = "booking_id"),
    @Index(name = "idx_saga_status", columnList = "status")
})
public class BookingSaga extends BaseEntity {

    @Column(name = "booking_id", nullable = false, unique = true)
    private Long bookingId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SagaStatus status = SagaStatus.STARTED;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_step", nullable = false)
    private SagaStep currentStep = SagaStep.SEATS_LOCKED;

    @Column(name = "payment_request_sent")
    private Boolean paymentRequestSent = false;

    @Column(name = "payment_request_sent_at")
    private LocalDateTime paymentRequestSentAt;

    @Column(name = "payment_completed")
    private Boolean paymentCompleted = false;

    @Column(name = "payment_completed_at")
    private LocalDateTime paymentCompletedAt;

    @Column(name = "seats_booked")
    private Boolean seatsBooked = false;

    @Column(name = "seats_booked_at")
    private LocalDateTime seatsBookedAt;

    @Column(name = "notification_sent")
    private Boolean notificationSent = false;

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Version
    private Long version;

    public BookingSaga() {}

    public BookingSaga(Long bookingId, LocalDateTime expiresAt) {
        this.bookingId = bookingId;
        this.expiresAt = expiresAt;
    }

    // Getters and Setters
    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public SagaStatus getStatus() {
        return status;
    }

    public void setStatus(SagaStatus status) {
        this.status = status;
    }

    public SagaStep getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(SagaStep currentStep) {
        this.currentStep = currentStep;
    }

    public Boolean getPaymentRequestSent() {
        return paymentRequestSent;
    }

    public void setPaymentRequestSent(Boolean paymentRequestSent) {
        this.paymentRequestSent = paymentRequestSent;
    }

    public LocalDateTime getPaymentRequestSentAt() {
        return paymentRequestSentAt;
    }

    public void setPaymentRequestSentAt(LocalDateTime paymentRequestSentAt) {
        this.paymentRequestSentAt = paymentRequestSentAt;
    }

    public Boolean getPaymentCompleted() {
        return paymentCompleted;
    }

    public void setPaymentCompleted(Boolean paymentCompleted) {
        this.paymentCompleted = paymentCompleted;
    }

    public LocalDateTime getPaymentCompletedAt() {
        return paymentCompletedAt;
    }

    public void setPaymentCompletedAt(LocalDateTime paymentCompletedAt) {
        this.paymentCompletedAt = paymentCompletedAt;
    }

    public Boolean getSeatsBooked() {
        return seatsBooked;
    }

    public void setSeatsBooked(Boolean seatsBooked) {
        this.seatsBooked = seatsBooked;
    }

    public LocalDateTime getSeatsBookedAt() {
        return seatsBookedAt;
    }

    public void setSeatsBookedAt(LocalDateTime seatsBookedAt) {
        this.seatsBookedAt = seatsBookedAt;
    }

    public Boolean getNotificationSent() {
        return notificationSent;
    }

    public void setNotificationSent(Boolean notificationSent) {
        this.notificationSent = notificationSent;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    // Business methods
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public void markPaymentRequestSent() {
        this.paymentRequestSent = true;
        this.paymentRequestSentAt = LocalDateTime.now();
        this.currentStep = SagaStep.PAYMENT_REQUESTED;
    }

    public void markPaymentCompleted() {
        this.paymentCompleted = true;
        this.paymentCompletedAt = LocalDateTime.now();
        this.currentStep = SagaStep.PAYMENT_COMPLETED;
    }

    public void markSeatsBooked() {
        this.seatsBooked = true;
        this.seatsBookedAt = LocalDateTime.now();
        this.currentStep = SagaStep.SEATS_CONFIRMED;
    }

    public void markNotificationSent() {
        this.notificationSent = true;
        this.currentStep = SagaStep.NOTIFICATION_SENT;
    }

    public void complete() {
        this.status = SagaStatus.COMPLETED;
    }

    public void fail(String error) {
        this.status = SagaStatus.FAILED;
        this.lastError = error;
    }

    public void compensate() {
        this.status = SagaStatus.COMPENSATING;
    }

    public void compensated() {
        this.status = SagaStatus.COMPENSATED;
    }

    public void incrementRetry() {
        this.retryCount++;
    }
}
