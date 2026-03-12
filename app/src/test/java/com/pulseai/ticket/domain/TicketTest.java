package com.pulseai.ticket.domain;

import com.pulseai.ticket.domain.model.Priority;
import com.pulseai.ticket.domain.model.Ticket;
import com.pulseai.ticket.domain.model.TicketId;
import com.pulseai.ticket.domain.model.TicketStatus;
import com.pulseai.ticket.domain.model.events.TicketAssignedEvent;
import com.pulseai.ticket.domain.model.events.TicketCreatedEvent;
import com.pulseai.ticket.domain.model.events.TicketResolvedEvent;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class TicketTest {

    @Test
    void shouldCreateTicketWithOpenStatus() {
        Ticket ticket = Ticket.create("Login issue", "Cannot login to portal", Priority.HIGH);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(ticket.getTitle()).isEqualTo("Login issue");
        assertThat(ticket.getPriority()).isEqualTo(Priority.HIGH);
        assertThat(ticket.getDomainEvents()).hasSize(1);
        assertThat(ticket.getDomainEvents().get(0)).isInstanceOf(TicketCreatedEvent.class);
    }

    @Test
    void shouldAssignTicketToAgent() {
        Ticket ticket = Ticket.create("DB error", "Database connection failed", Priority.CRITICAL);
        String agentId = UUID.randomUUID().toString();
        ticket.assignTo(agentId);
        assertThat(ticket.getAssignedAgentId()).isEqualTo(agentId);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        assertThat(ticket.getDomainEvents()).hasSize(2);
        assertThat(ticket.getDomainEvents().get(1)).isInstanceOf(TicketAssignedEvent.class);
    }

    @Test
    void shouldResolveTicket() {
        Ticket ticket = Ticket.create("UI bug", "Button misaligned", Priority.LOW);
        ticket.assignTo(UUID.randomUUID().toString());
        ticket.resolve("Fixed CSS class");
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.RESOLVED);
        assertThat(ticket.getResolution()).isEqualTo("Fixed CSS class");
        assertThat(ticket.getDomainEvents()).hasSize(3);
        assertThat(ticket.getDomainEvents().get(2)).isInstanceOf(TicketResolvedEvent.class);
    }

    @Test
    void shouldNotResolveOpenTicket() {
        Ticket ticket = Ticket.create("Auth error", "Token expired", Priority.HIGH);
        assertThatThrownBy(() -> ticket.resolve("Fixed"))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldNotAssignAlreadyAssignedTicket() {
        Ticket ticket = Ticket.create("Perf issue", "Slow query", Priority.MEDIUM);
        ticket.assignTo(UUID.randomUUID().toString());
        assertThatThrownBy(() -> ticket.assignTo(UUID.randomUUID().toString()))
            .isInstanceOf(IllegalStateException.class);
    }
}
