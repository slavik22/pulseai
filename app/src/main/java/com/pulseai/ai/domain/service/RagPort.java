package com.pulseai.ai.domain.service;

import com.pulseai.ai.domain.model.AiSuggestion;
import com.pulseai.ai.domain.model.KnowledgeArticle;

public interface RagPort {
    AiSuggestion generateSuggestion(String ticketId, String ticketContent, String category);
    void indexArticle(KnowledgeArticle article);
    void removeArticleEmbeddings(String articleId);
}
