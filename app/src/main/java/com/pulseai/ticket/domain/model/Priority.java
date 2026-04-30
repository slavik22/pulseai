package com.pulseai.ticket.domain.model;

public enum Priority {
    LOW(1), MEDIUM(2), HIGH(3), CRITICAL(4);

    private final int level;

    Priority(int level) { this.level = level; }

    public boolean isHigherThan(Priority other) {
        return this.level > other.level;
    }
}
