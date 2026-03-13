package com.pulseai.ticket.infrastructure.persistence.entity;

import com.pulseai.ticket.domain.model.Priority;
import com.pulseai.ticket.domain.model.TicketStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tickets", indexes = {
        @Index(name = "idx_tickets_customer_id", columnList = "customer_id"),
        @Index(name = "idx_tickets_status", columnList = "status"),
        @Index(name = "idx_tickets_agent_id", columnList = "assigned_agent_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TicketEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private TicketStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 50)
    private Priority priority;

    @Column(name = "customer_id", nullable = false, length = 255)
    private String customerId;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "assigned_agent_id", length = 255)
    private String assignedAgentId;

    @Column(name = "assigned_agent_name", length = 255)
    private String assignedAgentName;

    @Column(name = "resolution_note", columnDefinition = "TEXT")
    private String resolutionNote;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;
}
