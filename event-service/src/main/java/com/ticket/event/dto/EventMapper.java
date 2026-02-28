package com.ticket.event.dto;

import com.ticket.event.entity.Event;
import com.ticket.event.entity.Seat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class EventMapper {

    private EventMapper() {}

    public static EventResponse toResponse(Event event) {
        return new EventResponse(
            event.getId(),
            event.getName(),
            event.getDescription(),
            event.getVenue(),
            event.getVenueAddress(),
            event.getEventDate(),
            event.getDoorsOpenTime(),
            event.getEndTime(),
            event.getTotalSeats(),
            event.getAvailableSeats(),
            event.getBasePrice(),
            event.getStatus(),
            event.getCategory(),
            event.getImageUrl(),
            event.getOrganizerId(),
            event.getOrganizerName(),
            event.getMaxTicketsPerUser(),
            event.getSalesStartDate(),
            event.getSalesEndDate(),
            event.getCancellationDeadline(),
            event.getIsRefundable(),
            event.getRefundPercentage(),
            event.isSalesOpen(),
            toLocalDateTime(event.getCreatedAt()),
            toLocalDateTime(event.getUpdatedAt())
        );
    }
    
    private static LocalDateTime toLocalDateTime(Instant instant) {
        return instant != null ? LocalDateTime.ofInstant(instant, ZoneId.systemDefault()) : null;
    }

    public static SeatResponse toSeatResponse(Seat seat) {
        return new SeatResponse(
            seat.getId(),
            seat.getEvent().getId(),
            seat.getSection(),
            seat.getRowNumber(),
            seat.getSeatNumber(),
            seat.getType(),
            seat.getStatus(),
            seat.getPrice(),
            seat.getPriceMultiplier(),
            seat.getFinalPrice(),
            seat.getSeatIdentifier(),
            seat.isAvailable()
        );
    }

    public static Event toEntity(CreateEventRequest request, Long organizerId, String organizerName) {
        Event event = new Event();
        event.setName(request.name());
        event.setDescription(request.description());
        event.setVenue(request.venue());
        event.setVenueAddress(request.venueAddress());
        event.setEventDate(request.eventDate());
        event.setDoorsOpenTime(request.doorsOpenTime());
        event.setEndTime(request.endTime());
        event.setTotalSeats(request.totalSeats());
        event.setAvailableSeats(request.totalSeats());
        event.setBasePrice(request.basePrice());
        event.setCategory(request.category());
        event.setImageUrl(request.imageUrl());
        event.setOrganizerId(organizerId);
        event.setOrganizerName(organizerName);
        event.setMaxTicketsPerUser(request.maxTicketsPerUser());
        event.setSalesStartDate(request.salesStartDate());
        event.setSalesEndDate(request.salesEndDate());
        event.setCancellationDeadline(request.cancellationDeadline());
        event.setIsRefundable(request.isRefundable());
        event.setRefundPercentage(request.refundPercentage());
        return event;
    }

    public static void updateEntity(Event event, UpdateEventRequest request) {
        if (request.name() != null) event.setName(request.name());
        if (request.description() != null) event.setDescription(request.description());
        if (request.venue() != null) event.setVenue(request.venue());
        if (request.venueAddress() != null) event.setVenueAddress(request.venueAddress());
        if (request.eventDate() != null) event.setEventDate(request.eventDate());
        if (request.doorsOpenTime() != null) event.setDoorsOpenTime(request.doorsOpenTime());
        if (request.endTime() != null) event.setEndTime(request.endTime());
        if (request.basePrice() != null) event.setBasePrice(request.basePrice());
        if (request.category() != null) event.setCategory(request.category());
        if (request.imageUrl() != null) event.setImageUrl(request.imageUrl());
        if (request.maxTicketsPerUser() != null) event.setMaxTicketsPerUser(request.maxTicketsPerUser());
        if (request.salesStartDate() != null) event.setSalesStartDate(request.salesStartDate());
        if (request.salesEndDate() != null) event.setSalesEndDate(request.salesEndDate());
        if (request.cancellationDeadline() != null) event.setCancellationDeadline(request.cancellationDeadline());
        if (request.isRefundable() != null) event.setIsRefundable(request.isRefundable());
        if (request.refundPercentage() != null) event.setRefundPercentage(request.refundPercentage());
    }
}
