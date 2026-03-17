package com.pulseai.ai.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class KnowledgeArticle {

    private final UUID id;
    private String title;
    private String content;
    private String category;
    private boolean verifiedByExpert;
    private boolean indexed;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant indexedAt;

    public static KnowledgeArticle create(String title, String content, String category) {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(content, "content");
        if (content.length() < 50)
            throw new IllegalArgumentException("Article content too short (min 50 chars)");
        return new KnowledgeArticle(UUID.randomUUID(), title, content, category,
                false, false, Instant.now(), Instant.now(), null);
    }

    public static KnowledgeArticle reconstitute(UUID id, String title, String content,
                                                 String category, boolean verifiedByExpert,
                                                 boolean indexed, Instant createdAt,
                                                 Instant updatedAt, Instant indexedAt) {
        return new KnowledgeArticle(id, title, content, category,
                verifiedByExpert, indexed, createdAt, updatedAt, indexedAt);
    }

    private KnowledgeArticle(UUID id, String title, String content, String category,
                               boolean verifiedByExpert, boolean indexed,
                               Instant createdAt, Instant updatedAt, Instant indexedAt) {
        this.id = id; this.title = title; this.content = content; this.category = category;
        this.verifiedByExpert = verifiedByExpert; this.indexed = indexed;
        this.createdAt = createdAt; this.updatedAt = updatedAt; this.indexedAt = indexedAt;
    }

    public void markIndexed() {
        this.indexed = true;
        this.indexedAt = Instant.now();
        this.updatedAt = this.indexedAt;
    }

    public void update(String newContent) {
        this.content = newContent;
        this.indexed = false;
        this.indexedAt = null;
        this.updatedAt = Instant.now();
    }

    public boolean needsReIndexing() { return !indexed; }

    public UUID getId()                 { return id; }
    public String getTitle()            { return title; }
    public String getContent()          { return content; }
    public String getCategory()         { return category; }
    public boolean isVerifiedByExpert() { return verifiedByExpert; }
    public boolean isIndexed()          { return indexed; }
    public Instant getCreatedAt()       { return createdAt; }
    public Instant getUpdatedAt()       { return updatedAt; }
    public Instant getIndexedAt()       { return indexedAt; }
}
