package com.pulseai.ai.domain.repository;

import com.pulseai.ai.domain.model.KnowledgeArticle;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KnowledgeArticleRepository {
    KnowledgeArticle save(KnowledgeArticle article);
    Optional<KnowledgeArticle> findById(UUID id);
    List<KnowledgeArticle> findAll();
    List<KnowledgeArticle> findAllNeedingIndexing();
}
