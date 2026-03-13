package com.pulseai.ticket.application;

import com.pulseai.ticket.application.dto.AssignTicketCommand;
import com.pulseai.ticket.application.dto.CreateTicketCommand;
import com.pulseai.ticket.application.dto.ResolveTicketCommand;
import com.pulseai.ticket.application.dto.TicketResponse;
import com.pulseai.ticket.application.service.TicketApplicationService;
import com.pulseai.ticket.domain.model.Priority;
import com.pulseai.ticket.domain.model.TicketStatus;
import com.pulseai.ticket.domain.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.pulseai.ticket.domain.model.Ticket;

@ExtendWith(MockitoExtension.class)
class TicketApplicationServiceTest {

    private TicketRepository ticketRepository;
    private ApplicationEventPublisher eventPublisher;
    private TicketApplicationService service;

    @BeforeEach
    void setUp() {
        ticketRepository = mock(TicketRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        service = new TicketApplicationService(ticketRepository, eventPublisher);
    }

    @Test
    void shouldCreateTicket() {
        CreateTicketCommand cmd = new CreateTicketCommand("Login issue", "Cannot login", Priority.HIGH);
        when(ticketRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TicketResponse response = service.createTicket(cmd);

        assertThat(response.title()).isEqualTo("Login issue");
        assertThat(response.status()).isEqualTo(TicketStatus.OPEN);
        verify(ticketRepository).save(any(Ticket.class));
        verify(eventPublisher, atLeastOnce()).publishEvent(any());
    }

    @Test
    void shouldGetAllTickets() {
        Ticket t1 = Ticket.create("T1", "D1", Priority.LOW);
        Ticket t2 = Ticket.create("T2", "D2", Priority.HIGH);
        when(ticketRepository.findAll()).thenReturn(List.of(t1, t2));

        List<TicketResponse> result = service.getAllTickets();

        assertThat(result).hasSize(2);
    }
}
