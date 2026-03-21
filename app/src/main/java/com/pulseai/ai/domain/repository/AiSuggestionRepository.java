package com.pulseai.ai.domain.repository;

import com.pulseai.ai.domain.model.AiSuggestion;

import java.util.Optional;
import java.util.UUID;

public interface AiSuggestionRepository {
    AiSuggestion save(AiSuggestion suggestion);
    Optional<AiSuggestion> findById(UUID id);
    Optional<AiSuggestion> findByTicketId(String ticketId);
}
