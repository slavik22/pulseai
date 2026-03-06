package com.pulseai.ticket.domain.model.events;

import java.time.Instant;
import java.util.UUID;

public interface DomainEvent {
    UUID eventId();
    Instant occurredAt();
    String eventType();
}
