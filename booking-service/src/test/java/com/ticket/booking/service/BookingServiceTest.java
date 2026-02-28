package com.ticket.booking.service;

import com.ticket.booking.client.EventServiceClient;
import com.ticket.booking.entity.Booking;
import com.ticket.booking.entity.BookingItem;
import com.ticket.booking.entity.BookingStatus;
import com.ticket.booking.repository.BookingRepository;
import com.ticket.booking.repository.BookingSagaRepository;
import com.ticket.common.kafka.EventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Booking Service Unit Tests")
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingSagaRepository sagaRepository;

    @Mock
    private EventServiceClient eventServiceClient;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private BookingService bookingService;

    private Booking testBooking;

    @BeforeEach
    void setUp() {
        testBooking = new Booking();
        testBooking.setId(1L);
        testBooking.setUserId(100L);
        testBooking.setEventId(200L);
        testBooking.setStatus(BookingStatus.PENDING);
        testBooking.setTotalAmount(new BigDecimal("150.00"));
        testBooking.setConfirmationNumber("BK-TEST-001");
    }

    @Test
    @DisplayName("Should find booking by ID")
    void shouldFindBookingById() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));

        Optional<Booking> result = bookingService.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
        verify(bookingRepository).findById(1L);
    }

    @Test
    @DisplayName("Should find booking by confirmation number")
    void shouldFindByConfirmationNumber() {
        when(bookingRepository.findByConfirmationNumber("BK-TEST-001"))
            .thenReturn(Optional.of(testBooking));

        Optional<Booking> result = bookingService.findByConfirmationNumber("BK-TEST-001");

        assertThat(result).isPresent();
        assertThat(result.get().getConfirmationNumber()).isEqualTo("BK-TEST-001");
    }

    @Test
    @DisplayName("Should find bookings by user ID")
    void shouldFindBookingsByUserId() {
        List<Booking> bookings = Arrays.asList(testBooking);
        when(bookingRepository.findByUserIdOrderByCreatedAtDesc(100L)).thenReturn(bookings);

        List<Booking> result = bookingService.findByUserId(100L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("Should calculate total amount from booking items")
    void shouldCalculateTotalAmount() {
        BookingItem item1 = new BookingItem();
        item1.setPrice(new BigDecimal("50.00"));

        BookingItem item2 = new BookingItem();
        item2.setPrice(new BigDecimal("75.00"));

        BookingItem item3 = new BookingItem();
        item3.setPrice(new BigDecimal("50.00"));

        List<BookingItem> items = Arrays.asList(item1, item2, item3);

        BigDecimal total = bookingService.calculateTotal(items);

        assertThat(total).isEqualByComparingTo(new BigDecimal("175.00"));
    }

    @Test
    @DisplayName("Should transition booking status correctly")
    void shouldTransitionBookingStatus() {
        testBooking.setStatus(BookingStatus.PENDING);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));

        Booking result = bookingService.updateStatus(1L, BookingStatus.CONFIRMED);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        verify(bookingRepository).save(testBooking);
    }

    @Test
    @DisplayName("Should reject invalid status transition")
    void shouldRejectInvalidStatusTransition() {
        testBooking.setStatus(BookingStatus.CANCELLED);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));

        assertThatThrownBy(() -> bookingService.updateStatus(1L, BookingStatus.CONFIRMED))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Cannot transition");
    }
}
