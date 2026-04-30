package com.pulseai.ai.infrastructure.persistence.mapper;

import com.pulseai.ai.domain.model.AiSuggestion;
import com.pulseai.ai.domain.model.ConfidenceScore;
import com.pulseai.ai.infrastructure.persistence.entity.AiSuggestionEntity;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
public class AiSuggestionMapper {

    public AiSuggestionEntity toEntity(AiSuggestion s) {
        return AiSuggestionEntity.builder()
                .id(s.getId())
                .ticketId(s.getTicketId())
                .suggestedResponse(s.getSuggestedResponse())
                .confidenceScore(s.getConfidenceScore().value())
                .feedbackStatus(s.getFeedbackStatus())
                .agentEditedResponse(s.getAgentEditedResponse())
                .sourceArticleIds(String.join(",", s.getSourceArticleIds()))
                .createdAt(s.getCreatedAt())
                .feedbackAt(s.getFeedbackAt())
                .build();
    }

    public AiSuggestion toDomain(AiSuggestionEntity e) {
        List<String> sourceIds = (e.getSourceArticleIds() == null || e.getSourceArticleIds().isBlank())
                ? Collections.emptyList()
                : Arrays.asList(e.getSourceArticleIds().split(","));

        return AiSuggestion.reconstitute(e.getId(), e.getTicketId(), e.getSuggestedResponse(),
                ConfidenceScore.of(e.getConfidenceScore()), sourceIds, e.getFeedbackStatus(),
                e.getAgentEditedResponse(), e.getCreatedAt(), e.getFeedbackAt());
    }
}
