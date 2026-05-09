package com.pulseai.ticket.domain;

import com.pulseai.ticket.domain.model.Priority;
import com.pulseai.ticket.domain.model.Ticket;
import com.pulseai.ticket.domain.model.TicketStatus;
import com.pulseai.ticket.domain.model.events.DomainEvent;
import com.pulseai.ticket.domain.model.events.TicketAssignedEvent;
import com.pulseai.ticket.domain.model.events.TicketCreatedEvent;
import com.pulseai.ticket.domain.model.events.TicketResolvedEvent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class TicketTest {

    private static final String CUSTOMER_ID = UUID.randomUUID().toString();
    private static final String AGENT_ID = UUID.randomUUID().toString();
    private static final String AGENT_NAME = "Alice Smith";

    @Test
    void shouldOpenTicketWithOpenStatus() {
        Ticket ticket = Ticket.open("Login issue", "Cannot login to portal", Priority.HIGH, CUSTOMER_ID, "auth");

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(ticket.getTitle()).isEqualTo("Login issue");
        assertThat(ticket.getPriority()).isEqualTo(Priority.HIGH);
        assertThat(ticket.getCustomerId()).isEqualTo(CUSTOMER_ID);

        List<DomainEvent> events = ticket.pullDomainEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(TicketCreatedEvent.class);
    }

    @Test
    void shouldAssignTicketToAgent() {
        Ticket ticket = Ticket.open("DB error", "Database connection failed", Priority.CRITICAL, CUSTOMER_ID, "infra");
        ticket.pullDomainEvents(); // clear creation event

        ticket.assignTo(AGENT_ID, AGENT_NAME);

        assertThat(ticket.getAssignedAgentId()).isEqualTo(AGENT_ID);
        assertThat(ticket.getAssignedAgentName()).isEqualTo(AGENT_NAME);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);

        List<DomainEvent> events = ticket.pullDomainEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(TicketAssignedEvent.class);
    }

    @Test
    void shouldResolveAssignedTicket() {
        Ticket ticket = Ticket.open("UI bug", "Button misaligned", Priority.LOW, CUSTOMER_ID, "ui");
        ticket.assignTo(AGENT_ID, AGENT_NAME);
        ticket.pullDomainEvents(); // clear previous events

        ticket.resolve("Fixed CSS class");

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.RESOLVED);
        assertThat(ticket.getResolutionNote()).isEqualTo("Fixed CSS class");
        assertThat(ticket.getResolvedAt()).isNotNull();

        List<DomainEvent> events = ticket.pullDomainEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(TicketResolvedEvent.class);
    }

    @Test
    void shouldNotResolveUnassignedTicket() {
        Ticket ticket = Ticket.open("Auth error", "Token expired", Priority.HIGH, CUSTOMER_ID, "auth");
        assertThatThrownBy(() -> ticket.resolve("Fixed"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("unassigned");
    }

    @Test
    void shouldNotAssignAlreadyInProgressTicket() {
        Ticket ticket = Ticket.open("Perf issue", "Slow query", Priority.MEDIUM, CUSTOMER_ID, "db");
        ticket.assignTo(AGENT_ID, AGENT_NAME);
        assertThatThrownBy(() -> ticket.assignTo(UUID.randomUUID().toString(), "Bob"))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldRejectBlankTitle() {
        assertThatThrownBy(() -> Ticket.open("", "Some content", Priority.LOW, CUSTOMER_ID, "general"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("title");
    }

    @Test
    void pullDomainEventsClearsTheList() {
        Ticket ticket = Ticket.open("Test", "Content", Priority.LOW, CUSTOMER_ID, "general");
        assertThat(ticket.pullDomainEvents()).hasSize(1);
        assertThat(ticket.pullDomainEvents()).isEmpty();
    }
}
