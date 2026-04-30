package com.pulseai.ai.infrastructure.persistence.mapper;

import com.pulseai.ai.domain.model.KnowledgeArticle;
import com.pulseai.ai.infrastructure.persistence.entity.KnowledgeArticleEntity;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeArticleMapper {

    public KnowledgeArticleEntity toEntity(KnowledgeArticle a) {
        return KnowledgeArticleEntity.builder()
                .id(a.getId())
                .title(a.getTitle())
                .content(a.getContent())
                .category(a.getCategory())
                .verifiedByExpert(a.isVerifiedByExpert())
                .indexed(a.isIndexed())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .indexedAt(a.getIndexedAt())
                .build();
    }

    public KnowledgeArticle toDomain(KnowledgeArticleEntity e) {
        return KnowledgeArticle.reconstitute(e.getId(), e.getTitle(), e.getContent(),
                e.getCategory(), e.isVerifiedByExpert(), e.isIndexed(),
                e.getCreatedAt(), e.getUpdatedAt(), e.getIndexedAt());
    }
}
