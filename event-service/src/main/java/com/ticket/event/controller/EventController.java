package com.ticket.event.controller;

import com.ticket.common.security.UserPrincipal;
import com.ticket.event.dto.*;
import com.ticket.event.entity.EventCategory;
import com.ticket.event.service.EventService;
import com.ticket.event.service.SeatLockService;
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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/events")
public class EventController {

    private static final Logger logger = LoggerFactory.getLogger(EventController.class);

    private final EventService eventService;
    private final SeatLockService seatLockService;

    public EventController(EventService eventService, SeatLockService seatLockService) {
        this.eventService = eventService;
        this.seatLockService = seatLockService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @RateLimiter(name = "default")
    public ResponseEntity<EventResponse> createEvent(
            @Valid @RequestBody CreateEventRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        logger.info("Creating event: {} by user: {}", request.name(), principal.getUsername());
        EventResponse response = eventService.createEvent(
            request, 
            Long.valueOf(principal.getId()), 
            principal.getUsername()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventResponse> getEvent(@PathVariable Long eventId) {
        return ResponseEntity.ok(eventService.getEvent(eventId));
    }

    @GetMapping
    public ResponseEntity<Page<EventResponse>> getAllEvents(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(eventService.getAllEvents(pageable));
    }

    @GetMapping("/upcoming")
    public ResponseEntity<Page<EventResponse>> getUpcomingEvents(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(eventService.getUpcomingEvents(pageable));
    }

    @GetMapping("/available")
    public ResponseEntity<Page<EventResponse>> getAvailableEvents(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(eventService.getAvailableEvents(pageable));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<Page<EventResponse>> getEventsByCategory(
            @PathVariable EventCategory category,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(eventService.getEventsByCategory(category, pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<EventResponse>> searchEvents(
            @RequestParam String keyword,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(eventService.searchEvents(keyword, pageable));
    }

    @GetMapping("/organizer/{organizerId}")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN') or #organizerId == authentication.principal.id")
    public ResponseEntity<Page<EventResponse>> getEventsByOrganizer(
            @PathVariable Long organizerId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(eventService.getEventsByOrganizer(organizerId, pageable));
    }

    @GetMapping("/my-events")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<Page<EventResponse>> getMyEvents(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(eventService.getEventsByOrganizer(Long.valueOf(principal.getId()), pageable));
    }

    @PutMapping("/{eventId}")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<EventResponse> updateEvent(
            @PathVariable Long eventId,
            @Valid @RequestBody UpdateEventRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(eventService.updateEvent(eventId, request, Long.valueOf(principal.getId())));
    }

    @PostMapping("/{eventId}/publish")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<EventResponse> publishEvent(
            @PathVariable Long eventId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(eventService.publishEvent(eventId, Long.valueOf(principal.getId())));
    }

    @PostMapping("/{eventId}/cancel")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<EventResponse> cancelEvent(
            @PathVariable Long eventId,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        String reason = body != null ? body.get("reason") : "Event cancelled by organizer";
        return ResponseEntity.ok(eventService.cancelEvent(eventId, Long.valueOf(principal.getId()), reason));
    }

    @DeleteMapping("/{eventId}")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<Void> deleteEvent(
            @PathVariable Long eventId,
            @AuthenticationPrincipal UserPrincipal principal) {
        eventService.deleteEvent(eventId, Long.valueOf(principal.getId()));
        return ResponseEntity.noContent().build();
    }

    // Seat endpoints
    @GetMapping("/{eventId}/seats")
    public ResponseEntity<List<SeatResponse>> getSeats(@PathVariable Long eventId) {
        return ResponseEntity.ok(eventService.getSeatsForEvent(eventId));
    }

    @GetMapping("/{eventId}/seats/available")
    public ResponseEntity<List<SeatResponse>> getAvailableSeats(@PathVariable Long eventId) {
        return ResponseEntity.ok(eventService.getAvailableSeats(eventId));
    }

    @GetMapping("/{eventId}/seats/sections")
    public ResponseEntity<List<String>> getSections(@PathVariable Long eventId) {
        return ResponseEntity.ok(eventService.getSections(eventId));
    }

    @GetMapping("/{eventId}/seats/section/{section}")
    public ResponseEntity<List<SeatResponse>> getSeatsBySection(
            @PathVariable Long eventId,
            @PathVariable String section) {
        return ResponseEntity.ok(eventService.getSeatsBySection(eventId, section));
    }

    // Seat lock endpoints
    @PostMapping("/seats/lock")
    @PreAuthorize("isAuthenticated()")
    @RateLimiter(name = "default")
    public ResponseEntity<SeatLockResponse> lockSeats(
            @Valid @RequestBody LockSeatsRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        logger.info("User {} requesting to lock seats for event {}", 
                   principal.getId(), request.eventId());
        return ResponseEntity.ok(seatLockService.lockSeats(request, Long.valueOf(principal.getId())));
    }

    @DeleteMapping("/seats/lock/{lockId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> releaseLock(
            @PathVariable String lockId,
            @AuthenticationPrincipal UserPrincipal principal) {
        seatLockService.releaseLock(lockId, Long.valueOf(principal.getId()));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/seats/lock/{lockId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SeatLockService.SeatLockData> getLockStatus(
            @PathVariable String lockId) {
        SeatLockService.SeatLockData lockData = seatLockService.getLockData(lockId);
        if (lockData == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(lockData);
    }

    @DeleteMapping("/{eventId}/seats/my-locks")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> releaseMyLocks(
            @PathVariable Long eventId,
            @AuthenticationPrincipal UserPrincipal principal) {
        seatLockService.releaseUserLocks(Long.valueOf(principal.getId()), eventId);
        return ResponseEntity.noContent().build();
    }
}
