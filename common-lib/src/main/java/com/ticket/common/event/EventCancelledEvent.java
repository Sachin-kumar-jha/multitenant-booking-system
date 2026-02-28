package com.ticket.common.event;

/**
 * Event published when an event is cancelled.
 */
public class EventCancelledEvent extends BaseEvent {
    
    private String eventEntityId;
    private String name;
    private String cancellationReason;
    private boolean refundsRequired;
    private int affectedBookings;
    
    public EventCancelledEvent() {
        super();
    }
    
    public EventCancelledEvent(String tenantId, String correlationId, String eventEntityId,
                                String name, String cancellationReason, boolean refundsRequired,
                                int affectedBookings) {
        super(tenantId, correlationId);
        this.eventEntityId = eventEntityId;
        this.name = name;
        this.cancellationReason = cancellationReason;
        this.refundsRequired = refundsRequired;
        this.affectedBookings = affectedBookings;
    }
    
    public String getEventEntityId() {
        return eventEntityId;
    }
    
    public void setEventEntityId(String eventEntityId) {
        this.eventEntityId = eventEntityId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getCancellationReason() {
        return cancellationReason;
    }
    
    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }
    
    public boolean isRefundsRequired() {
        return refundsRequired;
    }
    
    public void setRefundsRequired(boolean refundsRequired) {
        this.refundsRequired = refundsRequired;
    }
    
    public int getAffectedBookings() {
        return affectedBookings;
    }
    
    public void setAffectedBookings(int affectedBookings) {
        this.affectedBookings = affectedBookings;
    }
}
