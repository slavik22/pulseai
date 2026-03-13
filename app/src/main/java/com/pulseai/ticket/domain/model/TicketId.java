package com.pulseai.ticket.domain.model;

import java.util.Objects;
import java.util.UUID;

public record TicketId(UUID value) {

    public TicketId {
        Objects.requireNonNull(value, "TicketId value must not be null");
    }

    public static TicketId generate() {
        return new TicketId(UUID.randomUUID());
    }

    public static TicketId of(UUID value) {
        return new TicketId(value);
    }

    public static TicketId of(String value) {
        return new TicketId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
