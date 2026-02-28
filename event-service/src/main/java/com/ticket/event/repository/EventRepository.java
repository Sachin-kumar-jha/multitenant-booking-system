package com.ticket.event.repository;

import com.ticket.event.entity.Event;
import com.ticket.event.entity.EventCategory;
import com.ticket.event.entity.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Event e WHERE e.id = :id")
    Optional<Event> findByIdWithLock(@Param("id") Long id);

    Page<Event> findByStatus(EventStatus status, Pageable pageable);

    Page<Event> findByCategory(EventCategory category, Pageable pageable);

    Page<Event> findByStatusAndCategory(EventStatus status, EventCategory category, Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.status = :status AND e.eventDate > :now ORDER BY e.eventDate ASC")
    Page<Event> findUpcomingEvents(@Param("status") EventStatus status, @Param("now") LocalDateTime now, Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.status = 'PUBLISHED' AND e.eventDate BETWEEN :startDate AND :endDate ORDER BY e.eventDate ASC")
    List<Event> findEventsBetweenDates(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT e FROM Event e WHERE e.organizerId = :organizerId ORDER BY e.createdAt DESC")
    Page<Event> findByOrganizerId(@Param("organizerId") Long organizerId, Pageable pageable);

    @Query("SELECT e FROM Event e WHERE LOWER(e.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(e.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Event> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.status = 'PUBLISHED' AND e.availableSeats > 0 AND e.eventDate > :now ORDER BY e.eventDate ASC")
    Page<Event> findAvailableEvents(@Param("now") LocalDateTime now, Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.status = 'PUBLISHED' AND e.venue LIKE %:venue% ORDER BY e.eventDate ASC")
    Page<Event> findByVenue(@Param("venue") String venue, Pageable pageable);

    @Query("SELECT COUNT(e) FROM Event e WHERE e.status = :status")
    long countByStatus(@Param("status") EventStatus status);

    @Query("SELECT e FROM Event e WHERE e.status = 'PUBLISHED' AND e.salesStartDate <= :now AND (e.salesEndDate IS NULL OR e.salesEndDate > :now) AND e.eventDate > :now ORDER BY e.eventDate ASC")
    Page<Event> findEventsOnSale(@Param("now") LocalDateTime now, Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.eventDate < :now AND e.status = 'PUBLISHED'")
    List<Event> findPastPublishedEvents(@Param("now") LocalDateTime now);
}
