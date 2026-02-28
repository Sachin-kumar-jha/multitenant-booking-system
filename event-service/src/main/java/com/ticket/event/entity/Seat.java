package com.ticket.event.entity;

import com.ticket.common.entity.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "seats", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"event_id", "section", "row_number", "seat_number"})
})
public class Seat extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false)
    private String section;

    @Column(name = "row_number", nullable = false)
    private String rowNumber;

    @Column(name = "seat_number", nullable = false)
    private String seatNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatType type = SeatType.REGULAR;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatStatus status = SeatStatus.AVAILABLE;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "price_multiplier", precision = 5, scale = 2)
    private BigDecimal priceMultiplier = BigDecimal.ONE;

    @Column(name = "locked_by")
    private Long lockedBy;

    @Column(name = "locked_until")
    private java.time.LocalDateTime lockedUntil;

    @Column(name = "booking_id")
    private Long bookingId;

    @Version
    private Long version;

    public Seat() {}

    public Seat(Event event, String section, String rowNumber, String seatNumber, SeatType type, BigDecimal price) {
        this.event = event;
        this.section = section;
        this.rowNumber = rowNumber;
        this.seatNumber = seatNumber;
        this.type = type;
        this.price = price;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(String rowNumber) {
        this.rowNumber = rowNumber;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    public SeatType getType() {
        return type;
    }

    public void setType(SeatType type) {
        this.type = type;
    }

    public SeatStatus getStatus() {
        return status;
    }

    public void setStatus(SeatStatus status) {
        this.status = status;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getPriceMultiplier() {
        return priceMultiplier;
    }

    public void setPriceMultiplier(BigDecimal priceMultiplier) {
        this.priceMultiplier = priceMultiplier;
    }

    public Long getLockedBy() {
        return lockedBy;
    }

    public void setLockedBy(Long lockedBy) {
        this.lockedBy = lockedBy;
    }

    public java.time.LocalDateTime getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(java.time.LocalDateTime lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public BigDecimal getFinalPrice() {
        return price.multiply(priceMultiplier);
    }

    public String getSeatIdentifier() {
        return String.format("%s-%s-%s", section, rowNumber, seatNumber);
    }

    public boolean isAvailable() {
        return status == SeatStatus.AVAILABLE;
    }

    public boolean isLocked() {
        return status == SeatStatus.LOCKED && 
               lockedUntil != null && 
               java.time.LocalDateTime.now().isBefore(lockedUntil);
    }

    public boolean isLockedByUser(Long userId) {
        return isLocked() && lockedBy != null && lockedBy.equals(userId);
    }

    public void lockForUser(Long userId, java.time.LocalDateTime until) {
        this.status = SeatStatus.LOCKED;
        this.lockedBy = userId;
        this.lockedUntil = until;
    }

    public void unlock() {
        this.status = SeatStatus.AVAILABLE;
        this.lockedBy = null;
        this.lockedUntil = null;
    }

    public void book(Long bookingId) {
        this.status = SeatStatus.BOOKED;
        this.bookingId = bookingId;
        this.lockedBy = null;
        this.lockedUntil = null;
    }

    public void release() {
        this.status = SeatStatus.AVAILABLE;
        this.bookingId = null;
        this.lockedBy = null;
        this.lockedUntil = null;
    }
}
