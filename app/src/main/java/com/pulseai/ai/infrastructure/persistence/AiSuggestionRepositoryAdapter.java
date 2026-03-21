package com.pulseai.ai.infrastructure.persistence;

import com.pulseai.ai.domain.model.AiSuggestion;
import com.pulseai.ai.domain.repository.AiSuggestionRepository;
import com.pulseai.ai.infrastructure.persistence.mapper.AiSuggestionMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class AiSuggestionRepositoryAdapter implements AiSuggestionRepository {

    private final AiSuggestionJpaRepository jpa;
    private final AiSuggestionMapper mapper;

    public AiSuggestionRepositoryAdapter(AiSuggestionJpaRepository jpa, AiSuggestionMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public AiSuggestion save(AiSuggestion s) {
        return mapper.toDomain(jpa.save(mapper.toEntity(s)));
    }

    @Override
    public Optional<AiSuggestion> findById(UUID id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<AiSuggestion> findByTicketId(String ticketId) {
        return jpa.findByTicketId(ticketId).map(mapper::toDomain);
    }
}
