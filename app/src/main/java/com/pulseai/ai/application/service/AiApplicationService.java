package com.pulseai.ai.application.service;

import com.pulseai.ai.application.dto.KnowledgeArticleResponse;
import com.pulseai.ai.application.dto.SuggestionResponse;
import com.pulseai.ai.domain.model.AiSuggestion;
import com.pulseai.ai.domain.model.KnowledgeArticle;
import com.pulseai.ai.domain.repository.AiSuggestionRepository;
import com.pulseai.ai.domain.repository.KnowledgeArticleRepository;
import com.pulseai.ai.domain.service.RagPort;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class AiApplicationService {

    private static final Logger log = LoggerFactory.getLogger(AiApplicationService.class);

    private final RagPort ragPort;
    private final AiSuggestionRepository suggestionRepository;
    private final KnowledgeArticleRepository articleRepository;
    private final Counter acceptedCounter;
    private final Counter rejectedCounter;
    private final Counter editedCounter;

    public AiApplicationService(RagPort ragPort, AiSuggestionRepository suggestionRepository,
                                 KnowledgeArticleRepository articleRepository,
                                 MeterRegistry meterRegistry) {
        this.ragPort = ragPort;
        this.suggestionRepository = suggestionRepository;
        this.articleRepository = articleRepository;
        this.acceptedCounter = Counter.builder("ai.suggestions.accepted").register(meterRegistry);
        this.rejectedCounter = Counter.builder("ai.suggestions.rejected").register(meterRegistry);
        this.editedCounter   = Counter.builder("ai.suggestions.edited").register(meterRegistry);
    }

    public SuggestionResponse generateSuggestion(String ticketId, String content, String category) {
        log.info("Generating RAG suggestion for ticket: {}", ticketId);
        AiSuggestion suggestion = ragPort.generateSuggestion(ticketId, content, category);
        return SuggestionResponse.from(suggestionRepository.save(suggestion));
    }

    @Transactional(readOnly = true)
    public SuggestionResponse getSuggestionByTicketId(String ticketId) {
        return suggestionRepository.findByTicketId(ticketId)
                .map(SuggestionResponse::from)
                .orElseThrow(() -> new SuggestionNotFoundException(ticketId));
    }

    public SuggestionResponse acceptSuggestion(UUID id) {
        AiSuggestion s = loadOrThrow(id);
        s.accept();
        acceptedCounter.increment();
        return SuggestionResponse.from(suggestionRepository.save(s));
    }

    public SuggestionResponse rejectSuggestion(UUID id) {
        AiSuggestion s = loadOrThrow(id);
        s.reject();
        rejectedCounter.increment();
        return SuggestionResponse.from(suggestionRepository.save(s));
    }

    public SuggestionResponse editSuggestion(UUID id, String editedResponse) {
        AiSuggestion s = loadOrThrow(id);
        s.editAndAccept(editedResponse);
        editedCounter.increment();
        return SuggestionResponse.from(suggestionRepository.save(s));
    }

    @Transactional(readOnly = true)
    public List<KnowledgeArticleResponse> getAllArticles() {
        return articleRepository.findAll().stream()
                .map(KnowledgeArticleResponse::from)
                .toList();
    }

    public void createAndIndexArticle(String title, String content, String category) {
        KnowledgeArticle article = KnowledgeArticle.create(title, content, category);
        KnowledgeArticle saved = articleRepository.save(article);
        try {
            ragPort.indexArticle(saved);
            saved.markIndexed();
            articleRepository.save(saved);
            log.info("Created and indexed article: {}", saved.getId());
        } catch (Exception e) {
            log.warn("Article {} saved but indexing failed (RAG unavailable): {}", saved.getId(), e.getMessage());
        }
    }

    private AiSuggestion loadOrThrow(UUID id) {
        return suggestionRepository.findById(id)
                .orElseThrow(() -> new SuggestionNotFoundException(id.toString()));
    }

    public static class SuggestionNotFoundException extends RuntimeException {
        public SuggestionNotFoundException(String id) { super("AI suggestion not found: " + id); }
    }
}
