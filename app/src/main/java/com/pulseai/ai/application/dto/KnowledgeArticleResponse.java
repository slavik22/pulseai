package com.pulseai.ai.application.dto;

import com.pulseai.ai.domain.model.KnowledgeArticle;

import java.time.Instant;
import java.util.UUID;

public record KnowledgeArticleResponse(
        UUID id,
        String title,
        String content,
        String category,
        boolean verifiedByExpert,
        boolean indexed,
        Instant createdAt,
        Instant updatedAt,
        Instant indexedAt
) {
    public static KnowledgeArticleResponse from(KnowledgeArticle a) {
        return new KnowledgeArticleResponse(
                a.getId(), a.getTitle(), a.getContent(), a.getCategory(),
                a.isVerifiedByExpert(), a.isIndexed(),
                a.getCreatedAt(), a.getUpdatedAt(), a.getIndexedAt()
        );
    }
}
