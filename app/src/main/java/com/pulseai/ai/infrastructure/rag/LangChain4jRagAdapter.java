package com.pulseai.ai.infrastructure.rag;

import com.pulseai.ai.domain.model.AiSuggestion;
import com.pulseai.ai.domain.model.ConfidenceScore;
import com.pulseai.ai.domain.model.KnowledgeArticle;
import com.pulseai.ai.domain.service.RagPort;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class LangChain4jRagAdapter implements RagPort {

    private static final Logger log = LoggerFactory.getLogger(LangChain4jRagAdapter.class);
    private static final int CHUNK_SIZE = 512;
    private static final int CHUNK_OVERLAP = 64;
    private static final int TOP_K = 5;
    private static final double MIN_SIMILARITY = 0.60;

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final ChatLanguageModel chatModel;
    private final Counter suggestionCounter;
    private final Counter lowConfidenceCounter;
    private final Timer ragTimer;

    public LangChain4jRagAdapter(EmbeddingModel embeddingModel,
                                  EmbeddingStore<TextSegment> embeddingStore,
                                  ChatLanguageModel chatModel,
                                  MeterRegistry meterRegistry) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.chatModel = chatModel;
        this.suggestionCounter   = Counter.builder("ai.suggestions.total").register(meterRegistry);
        this.lowConfidenceCounter = Counter.builder("ai.suggestions.low_confidence").register(meterRegistry);
        this.ragTimer = Timer.builder("ai.rag.latency").register(meterRegistry);
    }

    @Override
    public AiSuggestion generateSuggestion(String ticketId, String ticketContent, String category) {
        return ragTimer.record(() -> doGenerate(ticketId, ticketContent, category));
    }

    private AiSuggestion doGenerate(String ticketId, String ticketContent, String category) {
        suggestionCounter.increment();

        Embedding queryEmbedding = embeddingModel.embed(ticketContent).content();
        List<EmbeddingMatch<TextSegment>> matches =
                embeddingStore.findRelevant(queryEmbedding, TOP_K, MIN_SIMILARITY);

        double avgSimilarity = matches.isEmpty() ? 0.0
                : matches.stream().mapToDouble(EmbeddingMatch::score).average().orElse(0.0);
        ConfidenceScore confidence = ConfidenceScore.of(avgSimilarity);

        if (!confidence.isReliable()) {
            lowConfidenceCounter.increment();
            log.warn("Low confidence ({}) for ticket {}", confidence, ticketId);
        }

        String context = buildContext(matches);
        String draftResponse = generate(ticketContent, context, confidence);
        List<String> sourceIds = extractSourceIds(matches);

        return AiSuggestion.create(ticketId, draftResponse, confidence, sourceIds);
    }

    private String generate(String ticketContent, String context, ConfidenceScore confidence) {
        String system = """
                You are an expert customer support agent for a SaaS platform.
                Draft a helpful, empathetic response to the customer ticket below.
                Rules:
                1. Use ONLY the information in the CONTEXT section.
                2. If context is insufficient, say you will investigate and follow up.
                3. Do NOT invent facts, prices, or timelines.
                4. Be concise (2–4 paragraphs) and professional.
                5. End with: "Please let us know if you need anything else."
                %s
                CONTEXT:
                %s
                """.formatted(
                confidence.isReliable() ? "" : "\nWARNING: Limited context. Be conservative.",
                context.isEmpty() ? "No relevant knowledge base articles found." : context
        );

        return chatModel.generate(
                SystemMessage.from(system),
                UserMessage.from("CUSTOMER TICKET:\n" + ticketContent)
        ).content().text();
    }

    @Override
    public void indexArticle(KnowledgeArticle article) {
        var splitter = DocumentSplitters.recursive(CHUNK_SIZE, CHUNK_OVERLAP);
        Metadata metadata = Metadata.from(Map.of(
                "article_id", article.getId().toString(),
                "article_title", article.getTitle(),
                "category", article.getCategory() != null ? article.getCategory() : "general",
                "verified", String.valueOf(article.isVerifiedByExpert())
        ));
        Document document = Document.from(article.getTitle() + "\n\n" + article.getContent(), metadata);
        List<TextSegment> segments = splitter.split(document);
        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
        embeddingStore.addAll(embeddings, segments);
        log.info("Indexed {} chunks for article {}", segments.size(), article.getId());
    }

    @Override
    public void removeArticleEmbeddings(String articleId) {
        if (embeddingStore instanceof PgVectorEmbeddingStore pgStore) {
            pgStore.removeAll(MetadataFilterBuilder.metadataKey("article_id").isEqualTo(articleId));
        } else {
            log.warn("removeAll not supported by this EmbeddingStore — embeddings for article {} not removed", articleId);
        }
    }

    private String buildContext(List<EmbeddingMatch<TextSegment>> matches) {
        if (matches.isEmpty()) return "";
        return matches.stream().map(m -> {
            String title = m.embedded().metadata().getString("article_title");
            return "[%s] (%.0f%%)\n%s".formatted(
                    title != null ? title : "KB Article", m.score() * 100, m.embedded().text());
        }).collect(Collectors.joining("\n\n---\n\n"));
    }

    private List<String> extractSourceIds(List<EmbeddingMatch<TextSegment>> matches) {
        return matches.stream()
                .map(m -> m.embedded().metadata().getString("article_id"))
                .filter(id -> id != null)
                .distinct().toList();
    }
}
