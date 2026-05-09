package com.pulseai.ticket.application;

import com.pulseai.ticket.application.dto.CreateTicketCommand;
import com.pulseai.ticket.application.dto.TicketResponse;
import com.pulseai.ticket.application.service.TicketApplicationService;
import com.pulseai.ticket.domain.model.Priority;
import com.pulseai.ticket.domain.model.Ticket;
import com.pulseai.ticket.domain.model.TicketStatus;
import com.pulseai.ticket.domain.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketApplicationServiceTest {

    private TicketRepository ticketRepository;
    private ApplicationEventPublisher eventPublisher;
    private TicketApplicationService service;

    private static final String CUSTOMER_ID = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        ticketRepository = mock(TicketRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        service = new TicketApplicationService(ticketRepository, eventPublisher);
    }

    @Test
    void shouldCreateTicket() {
        CreateTicketCommand cmd = new CreateTicketCommand("Login issue", "Cannot login", Priority.HIGH, CUSTOMER_ID, "auth");
        when(ticketRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TicketResponse response = service.createTicket(cmd);

        assertThat(response.title()).isEqualTo("Login issue");
        assertThat(response.status()).isEqualTo(TicketStatus.OPEN);
        assertThat(response.customerId()).isEqualTo(CUSTOMER_ID);
        verify(ticketRepository).save(any(Ticket.class));
        verify(eventPublisher, atLeastOnce()).publishEvent(any());
    }

    @Test
    void shouldReturnTicketsByCustomer() {
        Ticket t1 = Ticket.open("T1", "D1", Priority.LOW, CUSTOMER_ID, "general");
        Ticket t2 = Ticket.open("T2", "D2", Priority.HIGH, CUSTOMER_ID, "auth");
        when(ticketRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(List.of(t1, t2));

        List<TicketResponse> result = service.getTicketsByCustomer(CUSTOMER_ID);

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(r -> r.customerId().equals(CUSTOMER_ID));
    }

    @Test
    void shouldReturnTicketsByAgent() {
        String agentId = UUID.randomUUID().toString();
        Ticket t1 = Ticket.open("T1", "D1", Priority.LOW, CUSTOMER_ID, "general");
        t1.assignTo(agentId, "Agent One");
        when(ticketRepository.findByAssignedAgentId(agentId)).thenReturn(List.of(t1));

        List<TicketResponse> result = service.getTicketsByAgent(agentId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).assignedAgentId()).isEqualTo(agentId);
    }

    @Test
    void shouldThrowWhenTicketNotFound() {
        when(ticketRepository.findById(any())).thenReturn(java.util.Optional.empty());
        assertThatThrownBy(() -> service.getTicketById(UUID.randomUUID().toString()))
            .isInstanceOf(TicketApplicationService.TicketNotFoundException.class);
    }
}
