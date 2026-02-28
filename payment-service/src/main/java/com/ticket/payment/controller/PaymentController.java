package com.ticket.payment.controller;

import com.ticket.common.security.UserPrincipal;
import com.ticket.payment.dto.PaymentResponse;
import com.ticket.payment.dto.ProcessPaymentRequest;
import com.ticket.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/process")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentResponse> processPayment(
            @Valid @RequestBody ProcessPaymentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        logger.info("Processing payment for booking {} by user {}", 
                   request.bookingId(), principal.getId());
        return ResponseEntity.ok(paymentService.processPayment(request, Long.valueOf(principal.getId())));
    }

    @GetMapping("/{paymentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentResponse> getPayment(
            @PathVariable Long paymentId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(paymentService.getPayment(paymentId, Long.valueOf(principal.getId())));
    }

    @GetMapping("/booking/{bookingId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentResponse> getPaymentByBooking(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(paymentService.getPaymentByBooking(bookingId, Long.valueOf(principal.getId())));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<PaymentResponse>> getMyPayments(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(paymentService.getUserPayments(Long.valueOf(principal.getId()), pageable));
    }

    @PostMapping("/{paymentId}/refund")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentResponse> initiateRefund(
            @PathVariable Long paymentId,
            @RequestBody(required = false) Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        BigDecimal amount = null;
        if (body != null && body.containsKey("amount")) {
            amount = new BigDecimal(body.get("amount").toString());
        }
        return ResponseEntity.ok(paymentService.initiateRefund(paymentId, Long.valueOf(principal.getId()), amount));
    }
}
