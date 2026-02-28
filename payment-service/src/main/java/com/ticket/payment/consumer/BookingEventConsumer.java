package com.ticket.payment.consumer;

import com.ticket.common.event.BookingRequestedEvent;
import com.ticket.common.kafka.IdempotentConsumer;
import com.ticket.common.kafka.KafkaTopics;
import com.ticket.common.tenant.TenantContext;
import com.ticket.payment.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class BookingEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(BookingEventConsumer.class);

    private final PaymentService paymentService;
    private final IdempotentConsumer idempotentConsumer;

    public BookingEventConsumer(PaymentService paymentService, IdempotentConsumer idempotentConsumer) {
        this.paymentService = paymentService;
        this.idempotentConsumer = idempotentConsumer;
    }

    @KafkaListener(topics = KafkaTopics.BOOKING_REQUESTED, groupId = "payment-service-group")
    public void handleBookingRequested(BookingRequestedEvent event) {
        logger.info("Received BookingRequested event for booking {}", event.getBookingId());

        // Set tenant context
        TenantContext.setCurrentTenant(event.getTenantId());

        try {
            // Check for idempotency
            if (!idempotentConsumer.tryProcess(event.getEventId(), "payment-service")) {
                logger.info("Event {} already processed, skipping", event.getEventId());
                return;
            }

            // Create payment record
            paymentService.createPaymentFromBooking(event);

            logger.info("Payment record created for booking {}", event.getBookingId());
        } catch (Exception e) {
            logger.error("Error processing BookingRequested event: {}", e.getMessage(), e);
            throw e;
        } finally {
            TenantContext.clear();
        }
    }
}
