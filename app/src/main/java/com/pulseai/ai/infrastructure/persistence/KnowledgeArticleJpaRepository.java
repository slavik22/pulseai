package com.pulseai.ai.infrastructure.persistence;

import com.pulseai.ai.infrastructure.persistence.entity.KnowledgeArticleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface KnowledgeArticleJpaRepository extends JpaRepository<KnowledgeArticleEntity, UUID> {
    List<KnowledgeArticleEntity> findByIndexedFalse();
}
