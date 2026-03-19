package com.pulseai.ai.infrastructure.persistence;

import com.pulseai.ai.domain.model.KnowledgeArticle;
import com.pulseai.ai.domain.repository.KnowledgeArticleRepository;
import com.pulseai.ai.infrastructure.persistence.mapper.KnowledgeArticleMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class KnowledgeArticleRepositoryAdapter implements KnowledgeArticleRepository {

    private final KnowledgeArticleJpaRepository jpa;
    private final KnowledgeArticleMapper mapper;

    public KnowledgeArticleRepositoryAdapter(KnowledgeArticleJpaRepository jpa, KnowledgeArticleMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public KnowledgeArticle save(KnowledgeArticle a) {
        return mapper.toDomain(jpa.save(mapper.toEntity(a)));
    }

    @Override
    public Optional<KnowledgeArticle> findById(UUID id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<KnowledgeArticle> findAll() {
        return jpa.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<KnowledgeArticle> findAllNeedingIndexing() {
        return jpa.findByIndexedFalse().stream().map(mapper::toDomain).toList();
    }
}
