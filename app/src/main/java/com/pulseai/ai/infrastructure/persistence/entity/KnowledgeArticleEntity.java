package com.pulseai.ai.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "knowledge_articles", indexes = {
        @Index(name = "idx_knowledge_articles_category", columnList = "category"),
        @Index(name = "idx_knowledge_articles_indexed", columnList = "indexed")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class KnowledgeArticleEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "verified_by_expert", nullable = false)
    private boolean verifiedByExpert;

    @Column(name = "indexed", nullable = false)
    private boolean indexed;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "indexed_at")
    private Instant indexedAt;
}
