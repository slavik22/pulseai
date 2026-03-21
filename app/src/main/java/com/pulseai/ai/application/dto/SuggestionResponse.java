package com.pulseai.ai.application.dto;

import com.pulseai.ai.domain.model.AiSuggestion;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SuggestionResponse(
        UUID id,
        String ticketId,
        String suggestedResponse,
        double confidenceScore,
        String confidenceLabel,
        boolean isReliable,
        List<String> sourceArticleIds,
        String feedbackStatus,
        Instant createdAt
) {
    public static SuggestionResponse from(AiSuggestion s) {
        return new SuggestionResponse(
                s.getId(), s.getTicketId(), s.getSuggestedResponse(),
                s.getConfidenceScore().value(), s.getConfidenceScore().label(),
                s.getConfidenceScore().isReliable(), s.getSourceArticleIds(),
                s.getFeedbackStatus().name(), s.getCreatedAt()
        );
    }
}
