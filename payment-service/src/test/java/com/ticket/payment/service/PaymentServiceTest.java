package com.ticket.payment.service;

import com.ticket.common.kafka.EventPublisher;
import com.ticket.common.kafka.IdempotentConsumer;
import com.ticket.payment.entity.Payment;
import com.ticket.payment.entity.PaymentMethod;
import com.ticket.payment.entity.PaymentStatus;
import com.ticket.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Payment Service Unit Tests")
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private IdempotentConsumer idempotentConsumer;

    @InjectMocks
    private PaymentService paymentService;

    private Payment testPayment;

    @BeforeEach
    void setUp() {
        testPayment = new Payment();
        testPayment.setId(1L);
        testPayment.setBookingId(100L);
        testPayment.setUserId(200L);
        testPayment.setAmount(new BigDecimal("150.00"));
        testPayment.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        testPayment.setStatus(PaymentStatus.PENDING);
    }

    @Test
    @DisplayName("Should create payment record")
    void shouldCreatePayment() {
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

        Payment result = paymentService.createPayment(100L, 200L, 
            new BigDecimal("150.00"), PaymentMethod.CREDIT_CARD);

        assertThat(result).isNotNull();
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("150.00"));
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    @DisplayName("Should find payment by ID")
    void shouldFindPaymentById() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));

        Optional<Payment> result = paymentService.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should find payment by booking ID")
    void shouldFindByBookingId() {
        when(paymentRepository.findByBookingId(100L)).thenReturn(Optional.of(testPayment));

        Optional<Payment> result = paymentService.findByBookingId(100L);

        assertThat(result).isPresent();
        assertThat(result.get().getBookingId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("Should mark payment as completed")
    void shouldMarkPaymentCompleted() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        Payment result = paymentService.markCompleted(1L, "TXN-123");

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(result.getTransactionReference()).isEqualTo("TXN-123");
    }

    @Test
    @DisplayName("Should mark payment as failed")
    void shouldMarkPaymentFailed() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        Payment result = paymentService.markFailed(1L, "Insufficient funds");

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(result.getFailureReason()).isEqualTo("Insufficient funds");
    }

    @Test
    @DisplayName("Should validate payment amount")
    void shouldValidatePaymentAmount() {
        assertThatThrownBy(() -> paymentService.createPayment(100L, 200L, 
                new BigDecimal("-10.00"), PaymentMethod.CREDIT_CARD))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Amount must be positive");
    }

    @Test
    @DisplayName("Should generate unique transaction reference")
    void shouldGenerateUniqueTransactionReference() {
        String ref1 = paymentService.generateTransactionReference();
        String ref2 = paymentService.generateTransactionReference();

        assertThat(ref1).isNotBlank();
        assertThat(ref2).isNotBlank();
        assertThat(ref1).isNotEqualTo(ref2);
    }
}
