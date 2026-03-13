package com.pulseai.ticket.domain.model;

import com.pulseai.ticket.domain.model.events.DomainEvent;
import com.pulseai.ticket.domain.model.events.TicketAssignedEvent;
import com.pulseai.ticket.domain.model.events.TicketCreatedEvent;
import com.pulseai.ticket.domain.model.events.TicketResolvedEvent;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Ticket {

    private final TicketId id;
    private String title;
    private String content;
    private TicketStatus status;
    private Priority priority;
    private final String customerId;
    private String category;
    private String assignedAgentId;
    private String assignedAgentName;
    private String resolutionNote;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant resolvedAt;

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    public static Ticket open(String title, String content, Priority priority,
                               String customerId, String category) {
        validateNotBlank(title, "title");
        validateNotBlank(content, "content");
        validateNotBlank(customerId, "customerId");
        Objects.requireNonNull(priority, "priority must not be null");

        Instant now = Instant.now();
        Ticket ticket = new Ticket(TicketId.generate(), title, content, TicketStatus.OPEN,
                priority, customerId, category, null, null, null, now, now, null);
        ticket.domainEvents.add(TicketCreatedEvent.of(
                ticket.id, title, content, priority, customerId, category));
        return ticket;
    }

    public static Ticket reconstitute(TicketId id, String title, String content,
                                       TicketStatus status, Priority priority, String customerId,
                                       String category, String assignedAgentId, String assignedAgentName,
                                       String resolutionNote, Instant createdAt, Instant updatedAt,
                                       Instant resolvedAt) {
        return new Ticket(id, title, content, status, priority, customerId, category,
                assignedAgentId, assignedAgentName, resolutionNote, createdAt, updatedAt, resolvedAt);
    }

    private Ticket(TicketId id, String title, String content, TicketStatus status,
                   Priority priority, String customerId, String category,
                   String assignedAgentId, String assignedAgentName, String resolutionNote,
                   Instant createdAt, Instant updatedAt, Instant resolvedAt) {
        this.id = id; this.title = title; this.content = content; this.status = status;
        this.priority = priority; this.customerId = customerId; this.category = category;
        this.assignedAgentId = assignedAgentId; this.assignedAgentName = assignedAgentName;
        this.resolutionNote = resolutionNote; this.createdAt = createdAt;
        this.updatedAt = updatedAt; this.resolvedAt = resolvedAt;
    }

    public void assignTo(String agentId, String agentName) {
        validateNotBlank(agentId, "agentId");
        validateNotBlank(agentName, "agentName");
        this.status = this.status.transitionTo(TicketStatus.IN_PROGRESS);
        this.assignedAgentId = agentId;
        this.assignedAgentName = agentName;
        this.updatedAt = Instant.now();
        domainEvents.add(TicketAssignedEvent.of(this.id, agentId, agentName));
    }

    public void resolve(String resolutionNote) {
        validateNotBlank(resolutionNote, "resolutionNote");
        if (this.assignedAgentId == null)
            throw new IllegalStateException("Cannot resolve an unassigned ticket");
        this.status = this.status.transitionTo(TicketStatus.RESOLVED);
        this.resolutionNote = resolutionNote;
        this.resolvedAt = Instant.now();
        this.updatedAt = this.resolvedAt;
        long minutes = ChronoUnit.MINUTES.between(createdAt, resolvedAt);
        domainEvents.add(TicketResolvedEvent.of(this.id, this.assignedAgentId, resolutionNote, minutes));
    }

    public void close() {
        this.status = this.status.transitionTo(TicketStatus.CLOSED);
        this.updatedAt = Instant.now();
    }

    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> events = List.copyOf(domainEvents);
        domainEvents.clear();
        return events;
    }

    public TicketId getId()               { return id; }
    public String getTitle()              { return title; }
    public String getContent()            { return content; }
    public TicketStatus getStatus()       { return status; }
    public Priority getPriority()         { return priority; }
    public String getCustomerId()         { return customerId; }
    public String getCategory()           { return category; }
    public String getAssignedAgentId()    { return assignedAgentId; }
    public String getAssignedAgentName()  { return assignedAgentName; }
    public String getResolutionNote()     { return resolutionNote; }
    public Instant getCreatedAt()         { return createdAt; }
    public Instant getUpdatedAt()         { return updatedAt; }
    public Instant getResolvedAt()        { return resolvedAt; }

    @Override public boolean equals(Object o) {
        if (!(o instanceof Ticket t)) return false;
        return Objects.equals(id, t.id);
    }
    @Override public int hashCode() { return Objects.hash(id); }

    private static void validateNotBlank(String value, String field) {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException(field + " must not be blank");
    }
}
