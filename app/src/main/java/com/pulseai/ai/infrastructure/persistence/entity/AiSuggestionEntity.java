package com.pulseai.ai.infrastructure.persistence.entity;

import com.pulseai.ai.domain.model.AiSuggestion.FeedbackStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_suggestions", indexes = {
        @Index(name = "idx_ai_suggestions_ticket_id", columnList = "ticket_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AiSuggestionEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "ticket_id", nullable = false, length = 255)
    private String ticketId;

    @Column(name = "suggested_response", nullable = false, columnDefinition = "TEXT")
    private String suggestedResponse;

    @Column(name = "confidence_score", nullable = false)
    private double confidenceScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "feedback_status", nullable = false, length = 50)
    private FeedbackStatus feedbackStatus;

    @Column(name = "agent_edited_response", columnDefinition = "TEXT")
    private String agentEditedResponse;

    @Column(name = "source_article_ids", columnDefinition = "TEXT")
    private String sourceArticleIds;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "feedback_at")
    private Instant feedbackAt;
}
