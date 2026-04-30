package com.pulseai.ticket.domain.model.events;

import com.pulseai.ticket.domain.model.Priority;
import com.pulseai.ticket.domain.model.TicketId;

import java.time.Instant;
import java.util.UUID;

public record TicketCreatedEvent(
        UUID eventId,
        Instant occurredAt,
        TicketId ticketId,
        String title,
        String content,
        Priority priority,
        String customerId,
        String category
) implements DomainEvent {

    public static TicketCreatedEvent of(TicketId ticketId, String title, String content,
                                        Priority priority, String customerId, String category) {
        return new TicketCreatedEvent(UUID.randomUUID(), Instant.now(),
                ticketId, title, content, priority, customerId, category);
    }

    @Override public String eventType() { return "ticket.created"; }
}
