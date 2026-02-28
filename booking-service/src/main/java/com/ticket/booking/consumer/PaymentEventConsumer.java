package com.ticket.booking.consumer;

import com.ticket.booking.service.BookingService;
import com.ticket.common.event.PaymentCompletedEvent;
import com.ticket.common.event.PaymentFailedEvent;
import com.ticket.common.kafka.IdempotentConsumer;
import com.ticket.common.kafka.KafkaTopics;
import com.ticket.common.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(PaymentEventConsumer.class);

    private final BookingService bookingService;
    private final IdempotentConsumer idempotentConsumer;

    public PaymentEventConsumer(BookingService bookingService, IdempotentConsumer idempotentConsumer) {
        this.bookingService = bookingService;
        this.idempotentConsumer = idempotentConsumer;
    }

    @KafkaListener(topics = KafkaTopics.PAYMENT_COMPLETED, groupId = "booking-service-group")
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        logger.info("Received PaymentCompleted event for booking {}", event.getBookingId());

        // Set tenant context
        TenantContext.setCurrentTenant(event.getTenantId());

        try {
            // Check for idempotency
            if (!idempotentConsumer.tryProcess(event.getEventId(), "booking-service")) {
                logger.info("Event {} already processed, skipping", event.getEventId());
                return;
            }

            bookingService.handlePaymentCompleted(
                Long.valueOf(event.getBookingId()),
                Long.valueOf(event.getPaymentId()),
                event.getTransactionRef()
            );

            logger.info("Successfully processed PaymentCompleted for booking {}", event.getBookingId());
        } catch (Exception e) {
            logger.error("Error processing PaymentCompleted event: {}", e.getMessage(), e);
            throw e;
        } finally {
            TenantContext.clear();
        }
    }

    @KafkaListener(topics = KafkaTopics.PAYMENT_FAILED, groupId = "booking-service-group")
    public void handlePaymentFailed(PaymentFailedEvent event) {
        logger.info("Received PaymentFailed event for booking {}", event.getBookingId());

        // Set tenant context
        TenantContext.setCurrentTenant(event.getTenantId());

        try {
            // Check for idempotency
            if (!idempotentConsumer.tryProcess(event.getEventId(), "booking-service")) {
                logger.info("Event {} already processed, skipping", event.getEventId());
                return;
            }

            bookingService.handlePaymentFailed(Long.valueOf(event.getBookingId()), event.getFailureReason());

            logger.info("Successfully processed PaymentFailed for booking {}", event.getBookingId());
        } catch (Exception e) {
            logger.error("Error processing PaymentFailed event: {}", e.getMessage(), e);
            throw e;
        } finally {
            TenantContext.clear();
        }
    }
}
