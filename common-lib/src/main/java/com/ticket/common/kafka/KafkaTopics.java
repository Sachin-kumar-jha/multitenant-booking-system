package com.ticket.common.kafka;

/**
 * Kafka topic constants for all services.
 */
public final class KafkaTopics {
    
    private KafkaTopics() {
        // Utility class
    }
    
    // Tenant events
    public static final String TENANT_CREATED = "tenant.created";
    public static final String TENANT_UPDATED = "tenant.updated";
    public static final String TENANT_SUSPENDED = "tenant.suspended";
    
    // Booking events
    public static final String BOOKING_REQUESTED = "booking.requested";
    public static final String BOOKING_CONFIRMED = "booking.confirmed";
    public static final String BOOKING_CANCELLED = "booking.cancelled";
    public static final String BOOKING_FAILED = "booking.failed";
    
    // Payment events
    public static final String PAYMENT_COMPLETED = "payment.completed";
    public static final String PAYMENT_FAILED = "payment.failed";
    public static final String PAYMENT_REFUND_REQUESTED = "payment.refund.requested";
    public static final String PAYMENT_REFUND_COMPLETED = "payment.refund.completed";
    
    // Event events (ticketing events)
    public static final String EVENT_CREATED = "event.created";
    public static final String EVENT_UPDATED = "event.updated";
    public static final String EVENT_CANCELLED = "event.cancelled";
    
    // Notification events
    public static final String NOTIFICATION_SEND = "notification.send";
    
    // Dead Letter Queues
    public static final String DLQ_BOOKING = "dlq.booking";
    public static final String DLQ_PAYMENT = "dlq.payment";
    public static final String DLQ_NOTIFICATION = "dlq.notification";
    
    // Consumer Groups
    public static final String CONSUMER_GROUP_PAYMENT = "payment-service-group";
    public static final String CONSUMER_GROUP_BOOKING = "booking-service-group";
    public static final String CONSUMER_GROUP_NOTIFICATION = "notification-service-group";
    public static final String CONSUMER_GROUP_EVENT = "event-service-group";
}
