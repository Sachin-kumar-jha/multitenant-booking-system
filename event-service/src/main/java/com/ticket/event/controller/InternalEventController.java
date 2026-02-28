package com.ticket.event.controller;

import com.ticket.event.dto.EventResponse;
import com.ticket.event.dto.SeatResponse;
import com.ticket.event.service.EventService;
import com.ticket.event.service.SeatLockService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Internal API for inter-service communication.
 * These endpoints should only be accessible from within the service mesh.
 */
@RestController
@RequestMapping("/internal/v1/events")
public class InternalEventController {

    private final EventService eventService;
    private final SeatLockService seatLockService;

    public InternalEventController(EventService eventService, SeatLockService seatLockService) {
        this.eventService = eventService;
        this.seatLockService = seatLockService;
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventResponse> getEvent(@PathVariable Long eventId) {
        return ResponseEntity.ok(eventService.getEvent(eventId));
    }

    @GetMapping("/{eventId}/seats")
    public ResponseEntity<List<SeatResponse>> getSeats(@PathVariable Long eventId) {
        return ResponseEntity.ok(eventService.getSeatsForEvent(eventId));
    }

    @PostMapping("/seats/book")
    public ResponseEntity<Void> bookSeats(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<Long> seatIds = ((List<Number>) request.get("seatIds"))
                .stream()
                .map(Number::longValue)
                .toList();
        Long bookingId = ((Number) request.get("bookingId")).longValue();
        
        eventService.bookSeats(seatIds, bookingId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/seats/release")
    public ResponseEntity<Void> releaseSeats(@RequestBody Map<String, Object> request) {
        Long bookingId = ((Number) request.get("bookingId")).longValue();
        eventService.releaseSeats(bookingId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/seats/lock/validate")
    public ResponseEntity<Map<String, Boolean>> validateLock(@RequestBody Map<String, Object> request) {
        String lockId = (String) request.get("lockId");
        Long userId = ((Number) request.get("userId")).longValue();
        @SuppressWarnings("unchecked")
        List<Long> seatIds = ((List<Number>) request.get("seatIds"))
                .stream()
                .map(Number::longValue)
                .toList();

        boolean valid = seatLockService.validateLock(lockId, userId, seatIds);
        return ResponseEntity.ok(Map.of("valid", valid));
    }

    @GetMapping("/seats/lock/{lockId}")
    public ResponseEntity<SeatLockService.SeatLockData> getLockData(@PathVariable String lockId) {
        SeatLockService.SeatLockData lockData = seatLockService.getLockData(lockId);
        if (lockData == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(lockData);
    }
}
