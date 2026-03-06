package com.pulseai.ticket.domain.model.events;

import com.pulseai.ticket.domain.model.TicketId;

import java.time.Instant;
import java.util.UUID;

public record TicketResolvedEvent(
        UUID eventId,
        Instant occurredAt,
        TicketId ticketId,
        String resolvedByAgentId,
        String resolutionNote,
        long resolutionMinutes
) implements DomainEvent {

    public static TicketResolvedEvent of(TicketId ticketId, String resolvedByAgentId,
                                         String resolutionNote, long resolutionMinutes) {
        return new TicketResolvedEvent(UUID.randomUUID(), Instant.now(),
                ticketId, resolvedByAgentId, resolutionNote, resolutionMinutes);
    }

    @Override public String eventType() { return "ticket.resolved"; }
}
