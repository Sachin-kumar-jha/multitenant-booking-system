package com.ticket.booking.client;

import com.ticket.booking.dto.EventData;
import com.ticket.booking.dto.SeatData;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "event-service", path = "/internal/v1/events")
public interface EventServiceClient {

    @GetMapping("/{eventId}")
    EventData getEvent(@PathVariable("eventId") Long eventId);

    @GetMapping("/{eventId}/seats")
    List<SeatData> getSeats(@PathVariable("eventId") Long eventId);

    @PostMapping("/seats/book")
    void bookSeats(@RequestBody Map<String, Object> request);

    @PostMapping("/seats/release")
    void releaseSeats(@RequestBody Map<String, Object> request);

    @PostMapping("/seats/lock/validate")
    Map<String, Boolean> validateLock(@RequestBody Map<String, Object> request);

    @GetMapping("/seats/lock/{lockId}")
    Map<String, Object> getLockData(@PathVariable("lockId") String lockId);
}
