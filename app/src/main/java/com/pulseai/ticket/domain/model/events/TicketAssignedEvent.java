package com.pulseai.ticket.domain.model.events;

import com.pulseai.ticket.domain.model.TicketId;

import java.time.Instant;
import java.util.UUID;

public record TicketAssignedEvent(
        UUID eventId,
        Instant occurredAt,
        TicketId ticketId,
        String agentId,
        String agentName
) implements DomainEvent {

    public static TicketAssignedEvent of(TicketId ticketId, String agentId, String agentName) {
        return new TicketAssignedEvent(UUID.randomUUID(), Instant.now(), ticketId, agentId, agentName);
    }

    @Override public String eventType() { return "ticket.assigned"; }
}
