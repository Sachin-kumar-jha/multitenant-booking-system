package com.ticket.event.service;

import com.ticket.common.event.EventCreatedEvent;
import com.ticket.common.event.EventCancelledEvent;
import com.ticket.common.exception.ResourceNotFoundException;
import com.ticket.common.exception.BusinessException;
import com.ticket.common.kafka.EventPublisher;
import com.ticket.common.kafka.KafkaTopics;
import com.ticket.common.tenant.TenantContext;
import com.ticket.event.dto.*;
import com.ticket.event.entity.*;
import com.ticket.event.repository.EventRepository;
import com.ticket.event.repository.SeatRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class EventService {

    private static final Logger logger = LoggerFactory.getLogger(EventService.class);

    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;
    private final EventPublisher eventPublisher;

    public EventService(EventRepository eventRepository, 
                       SeatRepository seatRepository,
                       EventPublisher eventPublisher) {
        this.eventRepository = eventRepository;
        this.seatRepository = seatRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    @CircuitBreaker(name = "default", fallbackMethod = "createEventFallback")
    public EventResponse createEvent(CreateEventRequest request, Long organizerId, String organizerName) {
        logger.info("Creating event '{}' for organizer {}", request.name(), organizerId);

        Event event = EventMapper.toEntity(request, organizerId, organizerName);
        event = eventRepository.save(event);

        // Create seats if seat sections are provided
        if (request.seatSections() != null && !request.seatSections().isEmpty()) {
            createSeatsForEvent(event, request.seatSections());
        } else {
            // Create default seats based on total seats
            createDefaultSeats(event, request.totalSeats());
        }

        // Publish event created
        publishEventCreated(event);

        logger.info("Event created with ID: {}", event.getId());
        return EventMapper.toResponse(event);
    }

    private void createSeatsForEvent(Event event, List<SeatSectionConfig> seatSections) {
        int totalSeatsCreated = 0;
        
        for (SeatSectionConfig config : seatSections) {
            for (int row = 1; row <= config.rows(); row++) {
                for (int seatNum = 1; seatNum <= config.seatsPerRow(); seatNum++) {
                    Seat seat = new Seat();
                    seat.setEvent(event);
                    seat.setSection(config.section());
                    seat.setRowNumber(String.valueOf(row));
                    seat.setSeatNumber(String.valueOf(seatNum));
                    seat.setType(config.seatType());
                    seat.setPrice(event.getBasePrice());
                    seat.setPriceMultiplier(config.priceMultiplier());
                    seat.setStatus(SeatStatus.AVAILABLE);
                    seatRepository.save(seat);
                    totalSeatsCreated++;
                }
            }
        }

        // Update event with actual total seats
        event.setTotalSeats(totalSeatsCreated);
        event.setAvailableSeats(totalSeatsCreated);
        eventRepository.save(event);
    }

    private void createDefaultSeats(Event event, int totalSeats) {
        int seatsPerRow = 20;
        int totalRows = (int) Math.ceil((double) totalSeats / seatsPerRow);
        int seatsCreated = 0;

        for (int row = 1; row <= totalRows && seatsCreated < totalSeats; row++) {
            int seatsInThisRow = Math.min(seatsPerRow, totalSeats - seatsCreated);
            for (int seatNum = 1; seatNum <= seatsInThisRow; seatNum++) {
                Seat seat = new Seat();
                seat.setEvent(event);
                seat.setSection("GENERAL");
                seat.setRowNumber(String.valueOf(row));
                seat.setSeatNumber(String.valueOf(seatNum));
                seat.setType(SeatType.REGULAR);
                seat.setPrice(event.getBasePrice());
                seat.setPriceMultiplier(BigDecimal.ONE);
                seat.setStatus(SeatStatus.AVAILABLE);
                seatRepository.save(seat);
                seatsCreated++;
            }
        }
    }

    private void publishEventCreated(Event event) {
        EventCreatedEvent eventCreatedEvent = new EventCreatedEvent(
            TenantContext.getTenantId(),
            UUID.randomUUID().toString(),
            event.getId().toString(),
            event.getName(),
            event.getVenue(),
            event.getEventDate().atZone(java.time.ZoneId.systemDefault()).toInstant(),
            event.getTotalSeats(),
            event.getBasePrice(),
            event.getOrganizerName()
        );
        eventPublisher.publish(KafkaTopics.EVENT_CREATED, event.getId().toString(), eventCreatedEvent);
    }

    public EventResponse createEventFallback(CreateEventRequest request, Long organizerId, String organizerName, Throwable t) {
        logger.error("Circuit breaker fallback for createEvent: {}", t.getMessage());
        throw new BusinessException("Event creation service is temporarily unavailable. Please try again later.");
    }

    @Transactional(readOnly = true)
    public EventResponse getEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));
        return EventMapper.toResponse(event);
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> getAllEvents(Pageable pageable) {
        return eventRepository.findAll(pageable).map(EventMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> getUpcomingEvents(Pageable pageable) {
        return eventRepository.findUpcomingEvents(EventStatus.PUBLISHED, LocalDateTime.now(), pageable)
                .map(EventMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> getEventsByCategory(EventCategory category, Pageable pageable) {
        return eventRepository.findByStatusAndCategory(EventStatus.PUBLISHED, category, pageable)
                .map(EventMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> getEventsByOrganizer(Long organizerId, Pageable pageable) {
        return eventRepository.findByOrganizerId(organizerId, pageable)
                .map(EventMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> searchEvents(String keyword, Pageable pageable) {
        return eventRepository.searchByKeyword(keyword, pageable)
                .map(EventMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> getAvailableEvents(Pageable pageable) {
        return eventRepository.findAvailableEvents(LocalDateTime.now(), pageable)
                .map(EventMapper::toResponse);
    }

    @Transactional
    public EventResponse updateEvent(Long eventId, UpdateEventRequest request, Long organizerId) {
        Event event = eventRepository.findByIdWithLock(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        // Verify organizer owns this event
        if (!event.getOrganizerId().equals(organizerId)) {
            throw new BusinessException("You don't have permission to update this event");
        }

        // Cannot update cancelled or completed events
        if (event.getStatus() == EventStatus.CANCELLED || event.getStatus() == EventStatus.COMPLETED) {
            throw new BusinessException("Cannot update an event that is " + event.getStatus());
        }

        EventMapper.updateEntity(event, request);
        event = eventRepository.save(event);

        logger.info("Event {} updated by organizer {}", eventId, organizerId);
        return EventMapper.toResponse(event);
    }

    @Transactional
    public EventResponse publishEvent(Long eventId, Long organizerId) {
        Event event = eventRepository.findByIdWithLock(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        if (!event.getOrganizerId().equals(organizerId)) {
            throw new BusinessException("You don't have permission to publish this event");
        }

        if (event.getStatus() != EventStatus.DRAFT) {
            throw new BusinessException("Only draft events can be published");
        }

        if (event.getEventDate().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Cannot publish an event with a past date");
        }

        event.setStatus(EventStatus.PUBLISHED);
        event = eventRepository.save(event);

        logger.info("Event {} published", eventId);
        return EventMapper.toResponse(event);
    }

    @Transactional
    public EventResponse cancelEvent(Long eventId, Long organizerId, String reason) {
        Event event = eventRepository.findByIdWithLock(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        if (!event.getOrganizerId().equals(organizerId)) {
            throw new BusinessException("You don't have permission to cancel this event");
        }

        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new BusinessException("Event is already cancelled");
        }

        if (event.getStatus() == EventStatus.COMPLETED) {
            throw new BusinessException("Cannot cancel a completed event");
        }

        event.setStatus(EventStatus.CANCELLED);
        event = eventRepository.save(event);

        // Release all booked seats
        seatRepository.releaseBookedSeats(eventId);

        // Publish event cancelled
        EventCancelledEvent eventCancelledEvent = new EventCancelledEvent(
            TenantContext.getTenantId(),
            UUID.randomUUID().toString(),
            eventId.toString(),
            event.getName(),
            reason,
            event.getIsRefundable(),
            0  // TODO: get actual affected bookings count
        );
        eventPublisher.publish(KafkaTopics.EVENT_CANCELLED, eventId.toString(), eventCancelledEvent);

        logger.info("Event {} cancelled. Reason: {}", eventId, reason);
        return EventMapper.toResponse(event);
    }

    @Transactional
    public void deleteEvent(Long eventId, Long organizerId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        if (!event.getOrganizerId().equals(organizerId)) {
            throw new BusinessException("You don't have permission to delete this event");
        }

        if (event.getStatus() == EventStatus.PUBLISHED) {
            throw new BusinessException("Cannot delete a published event. Cancel it first.");
        }

        // Check if there are any bookings
        long bookedSeats = seatRepository.countByEventIdAndStatus(eventId, SeatStatus.BOOKED);
        if (bookedSeats > 0) {
            throw new BusinessException("Cannot delete event with existing bookings");
        }

        eventRepository.delete(event);
        logger.info("Event {} deleted", eventId);
    }

    // Seat management methods
    @Transactional(readOnly = true)
    public List<SeatResponse> getSeatsForEvent(Long eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new ResourceNotFoundException("Event", "id", eventId);
        }
        return seatRepository.findByEventId(eventId).stream()
                .map(EventMapper::toSeatResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SeatResponse> getAvailableSeats(Long eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new ResourceNotFoundException("Event", "id", eventId);
        }
        return seatRepository.findByEventIdAndStatus(eventId, SeatStatus.AVAILABLE).stream()
                .map(EventMapper::toSeatResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SeatResponse> getSeatsBySection(Long eventId, String section) {
        return seatRepository.findByEventIdAndSection(eventId, section).stream()
                .map(EventMapper::toSeatResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> getSections(Long eventId) {
        return seatRepository.findSectionsByEventId(eventId);
    }

    // Internal method for booking service to update seat status
    @Transactional
    public void bookSeats(List<Long> seatIds, Long bookingId) {
        List<Seat> seats = seatRepository.findAllByIdWithLock(seatIds);
        
        for (Seat seat : seats) {
            if (!seat.isAvailable() && !seat.isLockedByUser(bookingId)) {
                throw new BusinessException("Seat " + seat.getSeatIdentifier() + " is no longer available");
            }
            seat.book(bookingId);
        }
        
        seatRepository.saveAll(seats);
        
        // Update event available seats count
        if (!seats.isEmpty()) {
            Event event = seats.get(0).getEvent();
            event.decrementAvailableSeats(seats.size());
            eventRepository.save(event);
        }
    }

    @Transactional
    public void releaseSeats(Long bookingId) {
        List<Seat> seats = seatRepository.findByBookingId(bookingId);
        
        if (!seats.isEmpty()) {
            Event event = seats.get(0).getEvent();
            
            for (Seat seat : seats) {
                seat.release();
            }
            
            seatRepository.saveAll(seats);
            event.incrementAvailableSeats(seats.size());
            eventRepository.save(event);
        }
    }

    // Helper methods for internal use and testing
    public Event createEvent(Event event) {
        return eventRepository.save(event);
    }

    public List<Seat> getAvailableSeatEntities(Long eventId) {
        return seatRepository.findByEventIdAndStatus(eventId, SeatStatus.AVAILABLE);
    }

    public void decrementAvailableSeats(Long eventId, int count) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        if (event.getAvailableSeats() < count) {
            throw new BusinessException("Not enough seats available");
        }

        event.decrementAvailableSeats(count);
        eventRepository.save(event);
    }
}
