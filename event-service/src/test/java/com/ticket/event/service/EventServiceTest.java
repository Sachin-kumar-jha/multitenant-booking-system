package com.ticket.event.service;

import com.ticket.common.exception.BusinessException;
import com.ticket.common.exception.ResourceNotFoundException;
import com.ticket.common.kafka.EventPublisher;
import com.ticket.common.tenant.TenantContext;
import com.ticket.event.dto.EventResponse;
import com.ticket.event.entity.Event;
import com.ticket.event.entity.EventStatus;
import com.ticket.event.entity.Seat;
import com.ticket.event.entity.SeatStatus;
import com.ticket.event.entity.SeatType;
import com.ticket.event.repository.EventRepository;
import com.ticket.event.repository.SeatRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Event Service Unit Tests")
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private EventService eventService;

    private MockedStatic<TenantContext> tenantContextMock;

    private Event testEvent;
    private List<Seat> testSeats;

    @BeforeEach
    void setUp() {
        tenantContextMock = mockStatic(TenantContext.class);
        tenantContextMock.when(TenantContext::getTenantId).thenReturn("tenant1");

        testEvent = new Event();
        testEvent.setId(1L);
        testEvent.setName("Test Concert");
        testEvent.setVenue("Test Arena");
        testEvent.setEventDate(LocalDateTime.now().plusDays(30));
        testEvent.setTotalSeats(100);
        testEvent.setAvailableSeats(100);
        testEvent.setBasePrice(new BigDecimal("50.00"));
        testEvent.setStatus(EventStatus.DRAFT);
        testEvent.setOrganizerId(1L);

        testSeats = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Seat seat = new Seat();
            seat.setId((long) i);
            seat.setEvent(testEvent);
            seat.setSeatNumber("A" + i);
            seat.setStatus(SeatStatus.AVAILABLE);
            seat.setPrice(new BigDecimal("50.00"));
            seat.setPriceMultiplier(BigDecimal.ONE);
            seat.setType(SeatType.REGULAR);
            testSeats.add(seat);
        }
    }

    @AfterEach
    void tearDown() {
        tenantContextMock.close();
    }

    @Test
    @DisplayName("Should create event successfully")
    void shouldCreateEvent() {
        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);

        Event result = eventService.createEvent(testEvent);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test Concert");
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    @DisplayName("Should get event by ID")
    void shouldGetEventById() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));

        EventResponse result = eventService.getEvent(1L);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should throw when event not found")
    void shouldThrowWhenEventNotFound() {
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.getEvent(999L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should publish event and change status")
    void shouldPublishEvent() {
        Long organizerId = 1L;
        testEvent.setStatus(EventStatus.DRAFT);
        when(eventRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testEvent));
        when(eventRepository.save(any(Event.class))).thenAnswer(i -> i.getArgument(0));

        EventResponse result = eventService.publishEvent(1L, organizerId);

        assertThat(result.status()).isEqualTo(EventStatus.PUBLISHED);
    }

    @Test
    @DisplayName("Should reject publishing already published event")
    void shouldRejectRepublishing() {
        Long organizerId = 1L;
        testEvent.setStatus(EventStatus.PUBLISHED);
        when(eventRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testEvent));

        assertThatThrownBy(() -> eventService.publishEvent(1L, organizerId))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("draft events can be published");
    }

    @Test
    @DisplayName("Should find available seats for event")
    void shouldFindAvailableSeats() {
        when(seatRepository.findByEventIdAndStatus(1L, SeatStatus.AVAILABLE))
            .thenReturn(testSeats);

        List<Seat> result = eventService.getAvailableSeatEntities(1L);

        assertThat(result).hasSize(5);
        assertThat(result).allMatch(s -> s.getStatus() == SeatStatus.AVAILABLE);
    }

    @Test
    @DisplayName("Should cancel event")
    void shouldCancelEvent() {
        Long organizerId = 1L;
        testEvent.setStatus(EventStatus.PUBLISHED);
        when(eventRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testEvent));
        when(eventRepository.save(any(Event.class))).thenAnswer(i -> i.getArgument(0));
        when(seatRepository.releaseBookedSeats(1L)).thenReturn(5);

        EventResponse result = eventService.cancelEvent(1L, organizerId, "Weather conditions");

        assertThat(result.status()).isEqualTo(EventStatus.CANCELLED);
        verify(eventPublisher).publish(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Should update available seats count")
    void shouldUpdateAvailableSeats() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(eventRepository.save(any(Event.class))).thenAnswer(i -> i.getArgument(0));

        eventService.decrementAvailableSeats(1L, 5);

        assertThat(testEvent.getAvailableSeats()).isEqualTo(95);
    }

    @Test
    @DisplayName("Should reject overbooking")
    void shouldRejectOverbooking() {
        testEvent.setAvailableSeats(3);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));

        assertThatThrownBy(() -> eventService.decrementAvailableSeats(1L, 5))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Not enough seats");
    }
}
