package com.ticket.event.entity;

import com.ticket.common.entity.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "events")
public class Event extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String venue;

    @Column(name = "venue_address")
    private String venueAddress;

    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    @Column(name = "doors_open_time")
    private LocalDateTime doorsOpenTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "total_seats", nullable = false)
    private Integer totalSeats;

    @Column(name = "available_seats", nullable = false)
    private Integer availableSeats;

    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status = EventStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_category")
    private EventCategory category;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "organizer_id")
    private Long organizerId;

    @Column(name = "organizer_name")
    private String organizerName;

    @Column(name = "max_tickets_per_user")
    private Integer maxTicketsPerUser = 10;

    @Column(name = "sales_start_date")
    private LocalDateTime salesStartDate;

    @Column(name = "sales_end_date")
    private LocalDateTime salesEndDate;

    @Column(name = "cancellation_deadline")
    private LocalDateTime cancellationDeadline;

    @Column(name = "is_refundable")
    private Boolean isRefundable = true;

    @Column(name = "refund_percentage")
    private Integer refundPercentage = 100;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Seat> seats = new ArrayList<>();

    @Version
    private Long version;

    public Event() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVenue() {
        return venue;
    }

    public void setVenue(String venue) {
        this.venue = venue;
    }

    public String getVenueAddress() {
        return venueAddress;
    }

    public void setVenueAddress(String venueAddress) {
        this.venueAddress = venueAddress;
    }

    public LocalDateTime getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDateTime eventDate) {
        this.eventDate = eventDate;
    }

    public LocalDateTime getDoorsOpenTime() {
        return doorsOpenTime;
    }

    public void setDoorsOpenTime(LocalDateTime doorsOpenTime) {
        this.doorsOpenTime = doorsOpenTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Integer getTotalSeats() {
        return totalSeats;
    }

    public void setTotalSeats(Integer totalSeats) {
        this.totalSeats = totalSeats;
    }

    public Integer getAvailableSeats() {
        return availableSeats;
    }

    public void setAvailableSeats(Integer availableSeats) {
        this.availableSeats = availableSeats;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }

    public EventCategory getCategory() {
        return category;
    }

    public void setCategory(EventCategory category) {
        this.category = category;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Long getOrganizerId() {
        return organizerId;
    }

    public void setOrganizerId(Long organizerId) {
        this.organizerId = organizerId;
    }

    public String getOrganizerName() {
        return organizerName;
    }

    public void setOrganizerName(String organizerName) {
        this.organizerName = organizerName;
    }

    public Integer getMaxTicketsPerUser() {
        return maxTicketsPerUser;
    }

    public void setMaxTicketsPerUser(Integer maxTicketsPerUser) {
        this.maxTicketsPerUser = maxTicketsPerUser;
    }

    public LocalDateTime getSalesStartDate() {
        return salesStartDate;
    }

    public void setSalesStartDate(LocalDateTime salesStartDate) {
        this.salesStartDate = salesStartDate;
    }

    public LocalDateTime getSalesEndDate() {
        return salesEndDate;
    }

    public void setSalesEndDate(LocalDateTime salesEndDate) {
        this.salesEndDate = salesEndDate;
    }

    public LocalDateTime getCancellationDeadline() {
        return cancellationDeadline;
    }

    public void setCancellationDeadline(LocalDateTime cancellationDeadline) {
        this.cancellationDeadline = cancellationDeadline;
    }

    public Boolean getIsRefundable() {
        return isRefundable;
    }

    public void setIsRefundable(Boolean isRefundable) {
        this.isRefundable = isRefundable;
    }

    public Integer getRefundPercentage() {
        return refundPercentage;
    }

    public void setRefundPercentage(Integer refundPercentage) {
        this.refundPercentage = refundPercentage;
    }

    public List<Seat> getSeats() {
        return seats;
    }

    public void setSeats(List<Seat> seats) {
        this.seats = seats;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public void addSeat(Seat seat) {
        seats.add(seat);
        seat.setEvent(this);
    }

    public void removeSeat(Seat seat) {
        seats.remove(seat);
        seat.setEvent(null);
    }

    public boolean isSalesOpen() {
        LocalDateTime now = LocalDateTime.now();
        boolean afterStart = salesStartDate == null || now.isAfter(salesStartDate);
        boolean beforeEnd = salesEndDate == null || now.isBefore(salesEndDate);
        return afterStart && beforeEnd && status == EventStatus.PUBLISHED;
    }

    public boolean isCancellable() {
        return cancellationDeadline == null || LocalDateTime.now().isBefore(cancellationDeadline);
    }

    public void decrementAvailableSeats(int count) {
        if (this.availableSeats < count) {
            throw new IllegalStateException("Not enough available seats");
        }
        this.availableSeats -= count;
    }

    public void incrementAvailableSeats(int count) {
        this.availableSeats = Math.min(this.availableSeats + count, this.totalSeats);
    }
}
