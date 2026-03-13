package com.pulseai.ticket.domain.model;

public enum TicketStatus {
    OPEN {
        @Override
        public TicketStatus transitionTo(TicketStatus next) {
            if (next == IN_PROGRESS || next == CLOSED) return next;
            throw new IllegalStateTransitionException(this, next);
        }
    },
    IN_PROGRESS {
        @Override
        public TicketStatus transitionTo(TicketStatus next) {
            if (next == RESOLVED || next == OPEN) return next;
            throw new IllegalStateTransitionException(this, next);
        }
    },
    RESOLVED {
        @Override
        public TicketStatus transitionTo(TicketStatus next) {
            if (next == CLOSED || next == OPEN) return next;
            throw new IllegalStateTransitionException(this, next);
        }
    },
    CLOSED {
        @Override
        public TicketStatus transitionTo(TicketStatus next) {
            throw new IllegalStateTransitionException(this, next);
        }
    };

    public abstract TicketStatus transitionTo(TicketStatus next);

    public boolean isTerminal() { return this == CLOSED; }

    public static class IllegalStateTransitionException extends RuntimeException {
        public IllegalStateTransitionException(TicketStatus from, TicketStatus to) {
            super("Cannot transition ticket from %s to %s".formatted(from, to));
        }
    }
}
