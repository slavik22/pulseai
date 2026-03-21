package com.pulseai.ai.infrastructure.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import dev.langchain4j.data.segment.TextSegment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LangChain4jConfig {

    @Value("${ai.openai.api-key}")
    private String apiKey;

    @Value("${ai.openai.chat-model}")
    private String chatModel;

    // Injected separately so we don't have to parse the JDBC URL ourselves.
    @Value("${spring.datasource.host:postgres}")
    private String dbHost;

    @Value("${spring.datasource.port:5432}")
    private int dbPort;

    @Value("${spring.datasource.username}")
    private String dbUser;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Value("${spring.datasource.dbname:pulseai}")
    private String dbName;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(chatModel)
                .temperature(0.3)
                .maxTokens(1000)
                .build();
    }

    @Bean
    public EmbeddingModel embeddingModel() {
        return new AllMiniLmL6V2EmbeddingModel();
    }

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        return PgVectorEmbeddingStore.builder()
                .host(dbHost)
                .port(dbPort)
                .user(dbUser)
                .password(dbPassword)
                .database(dbName)
                .table("langchain4j_embeddings")
                .dimension(384)
                .createTable(true)
                .build();
    }
}
