package com.ticket.booking.controller;

import com.ticket.booking.dto.BookingResponse;
import com.ticket.booking.dto.CreateBookingRequest;
import com.ticket.booking.entity.BookingStatus;
import com.ticket.booking.service.BookingService;
import com.ticket.common.security.UserPrincipal;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

    private static final Logger logger = LoggerFactory.getLogger(BookingController.class);

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @RateLimiter(name = "default")
    public ResponseEntity<BookingResponse> createBooking(
            @Valid @RequestBody CreateBookingRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        logger.info("Creating booking for user {} for event {}", 
                   principal.getId(), request.eventId());
        
        BookingResponse response = bookingService.createBooking(
            request,
            Long.valueOf(principal.getId()),
            principal.getEmail(),
            principal.getUsername()
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{bookingId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BookingResponse> getBooking(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(bookingService.getBooking(bookingId, Long.valueOf(principal.getId())));
    }

    @GetMapping("/confirmation/{confirmationNumber}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BookingResponse> getBookingByConfirmation(
            @PathVariable String confirmationNumber,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(
            bookingService.getBookingByConfirmation(confirmationNumber, Long.valueOf(principal.getId()))
        );
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<BookingResponse>> getMyBookings(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(bookingService.getUserBookings(Long.valueOf(principal.getId()), pageable));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<BookingResponse>> getMyBookingsByStatus(
            @PathVariable BookingStatus status,
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(
            bookingService.getUserBookingsByStatus(Long.valueOf(principal.getId()), status, pageable)
        );
    }

    @PostMapping("/{bookingId}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BookingResponse> cancelBooking(
            @PathVariable Long bookingId,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        String reason = body != null ? body.get("reason") : "Cancelled by user";
        return ResponseEntity.ok(
            bookingService.cancelBooking(bookingId, Long.valueOf(principal.getId()), reason)
        );
    }
}
