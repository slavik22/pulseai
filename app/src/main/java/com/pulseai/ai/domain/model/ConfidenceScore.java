package com.pulseai.ai.domain.model;

public record ConfidenceScore(double value) {

    public ConfidenceScore {
        if (value < 0.0 || value > 1.0)
            throw new IllegalArgumentException("Confidence score must be 0–1, got: " + value);
    }

    public static ConfidenceScore of(double value) { return new ConfidenceScore(value); }

    public boolean isReliable() { return value >= 0.70; }

    public String label() {
        if (value >= 0.85) return "HIGH";
        if (value >= 0.70) return "MEDIUM";
        return "LOW";
    }

    @Override
    public String toString() { return "%.2f (%s)".formatted(value, label()); }
}
