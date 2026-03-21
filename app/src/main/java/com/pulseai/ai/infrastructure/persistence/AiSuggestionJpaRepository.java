package com.pulseai.ai.infrastructure.persistence;

import com.pulseai.ai.infrastructure.persistence.entity.AiSuggestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AiSuggestionJpaRepository extends JpaRepository<AiSuggestionEntity, UUID> {
    Optional<AiSuggestionEntity> findByTicketId(String ticketId);
}
