package com.ticket.event.service;

import com.ticket.common.exception.BusinessException;
import com.ticket.common.exception.ResourceNotFoundException;
import com.ticket.common.redis.RedisLockService;
import com.ticket.common.tenant.TenantContext;
import com.ticket.event.dto.LockSeatsRequest;
import com.ticket.event.dto.SeatLockResponse;
import com.ticket.event.entity.Event;
import com.ticket.event.entity.EventStatus;
import com.ticket.event.entity.Seat;
import com.ticket.event.entity.SeatStatus;
import com.ticket.event.repository.EventRepository;
import com.ticket.event.repository.SeatRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Seat Lock Service Unit Tests")
class SeatLockServiceTest {

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private RedisLockService redisLockService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private SeatLockService seatLockService;

    private MockedStatic<TenantContext> tenantContextMock;

    private static final Long USER_ID = 100L;
    private static final Long EVENT_ID = 1L;
    private static final List<Long> SEAT_IDS = List.of(1L, 2L, 3L);

    private Event testEvent;
    private List<Seat> testSeats;

    @BeforeEach
    void setUp() {
        seatLockService = new SeatLockService(seatRepository, eventRepository, redisLockService, redisTemplate);
        ReflectionTestUtils.setField(seatLockService, "lockTtlSeconds", 600);
        ReflectionTestUtils.setField(seatLockService, "maxSeatsPerBooking", 10);

        tenantContextMock = mockStatic(TenantContext.class);
        tenantContextMock.when(TenantContext::getTenantId).thenReturn("tenant1");

        testEvent = new Event();
        testEvent.setId(EVENT_ID);
        testEvent.setName("Test Event");
        testEvent.setStatus(EventStatus.PUBLISHED);
        testEvent.setSalesStartDate(LocalDateTime.now().minusDays(1));
        testEvent.setSalesEndDate(LocalDateTime.now().plusDays(30));
        testEvent.setMaxTicketsPerUser(10);

        testSeats = SEAT_IDS.stream().map(id -> {
            Seat seat = new Seat();
            seat.setId(id);
            seat.setEvent(testEvent);
            seat.setStatus(SeatStatus.AVAILABLE);
            seat.setPrice(new BigDecimal("50.00"));
            seat.setPriceMultiplier(BigDecimal.ONE);
            return seat;
        }).toList();
    }

    @AfterEach
    void tearDown() {
        tenantContextMock.close();
    }

    @Test
    @DisplayName("Should lock seats successfully")
    void shouldLockSeatsSuccessfully() {
        LockSeatsRequest request = new LockSeatsRequest(EVENT_ID, SEAT_IDS);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(testEvent));
        when(redisLockService.tryLock(anyString(), any(Duration.class))).thenReturn(true);
        when(seatRepository.findAllByIdWithLock(SEAT_IDS)).thenReturn(testSeats);
        when(seatRepository.findLockedSeatsByUserAndEvent(USER_ID, EVENT_ID)).thenReturn(List.of());
        when(seatRepository.saveAll(anyList())).thenReturn(testSeats);

        SeatLockResponse result = seatLockService.lockSeats(request, USER_ID);

        assertThat(result).isNotNull();
        assertThat(result.seatIds()).hasSize(3);
        verify(redisLockService).tryLock(anyString(), any(Duration.class));
        verify(redisLockService).unlock(anyString());
    }

    @Test
    @DisplayName("Should fail if event not found")
    void shouldFailIfEventNotFound() {
        LockSeatsRequest request = new LockSeatsRequest(EVENT_ID, SEAT_IDS);

        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> seatLockService.lockSeats(request, USER_ID))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should fail if event is not published")
    void shouldFailIfEventNotPublished() {
        testEvent.setStatus(EventStatus.DRAFT);
        LockSeatsRequest request = new LockSeatsRequest(EVENT_ID, SEAT_IDS);

        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(testEvent));

        assertThatThrownBy(() -> seatLockService.lockSeats(request, USER_ID))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("not available");
    }

    @Test
    @DisplayName("Should fail if too many seats requested")
    void shouldFailIfTooManySeats() {
        List<Long> tooManySeats = List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L);
        LockSeatsRequest request = new LockSeatsRequest(EVENT_ID, tooManySeats);

        assertThatThrownBy(() -> seatLockService.lockSeats(request, USER_ID))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Cannot lock more than");
    }
}
