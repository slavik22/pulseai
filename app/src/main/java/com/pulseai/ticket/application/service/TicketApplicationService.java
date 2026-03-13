package com.pulseai.ticket.application.service;

import com.pulseai.ticket.application.dto.*;
import com.pulseai.ticket.application.port.in.*;
import com.pulseai.ticket.domain.model.Ticket;
import com.pulseai.ticket.domain.model.TicketId;
import com.pulseai.ticket.domain.repository.TicketRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * WHY ApplicationEventPublisher instead of Kafka?
 *
 * In a monolith, cross-module communication happens in-process.
 * Spring's ApplicationEventPublisher dispatches events to any @EventListener
 * in the same JVM — no network, no serialization, no broker needed.
 *
 * With @TransactionalEventListener(phase = AFTER_COMMIT):
 * Listeners only fire after the DB transaction commits successfully.
 * If save() fails → no events fired. Consistent by design.
 *
 * With @Async on the listeners:
 * AI suggestion generation and WebSocket notifications happen in a separate
 * thread — the HTTP response returns immediately without waiting for OpenAI.
 */
@Service
@Transactional
public class TicketApplicationService
        implements CreateTicketUseCase, AssignTicketUseCase, ResolveTicketUseCase, GetTicketUseCase {

    private final TicketRepository ticketRepository;
    private final ApplicationEventPublisher eventPublisher;

    public TicketApplicationService(TicketRepository ticketRepository,
                                    ApplicationEventPublisher eventPublisher) {
        this.ticketRepository = ticketRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public TicketResponse createTicket(CreateTicketCommand command) {
        Ticket ticket = Ticket.open(command.title(), command.content(),
                command.priority(), command.customerId(), command.category());
        var events = ticket.pullDomainEvents();
        Ticket saved = ticketRepository.save(ticket);
        events.forEach(eventPublisher::publishEvent);
        return TicketResponse.from(saved);
    }

    @Override
    public TicketResponse assignTicket(AssignTicketCommand command) {
        Ticket ticket = loadOrThrow(command.ticketId());
        ticket.assignTo(command.agentId(), command.agentName());
        var events = ticket.pullDomainEvents();
        Ticket saved = ticketRepository.save(ticket);
        events.forEach(eventPublisher::publishEvent);
        return TicketResponse.from(saved);
    }

    @Override
    public TicketResponse resolveTicket(ResolveTicketCommand command) {
        Ticket ticket = loadOrThrow(command.ticketId());
        ticket.resolve(command.resolutionNote());
        var events = ticket.pullDomainEvents();
        Ticket saved = ticketRepository.save(ticket);
        events.forEach(eventPublisher::publishEvent);
        return TicketResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse getTicketById(String ticketId) {
        return TicketResponse.from(loadOrThrow(ticketId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> getTicketsByCustomer(String customerId) {
        return ticketRepository.findByCustomerId(customerId).stream()
                .map(TicketResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> getTicketsByAgent(String agentId) {
        return ticketRepository.findByAssignedAgentId(agentId).stream()
                .map(TicketResponse::from).toList();
    }

    private Ticket loadOrThrow(String ticketId) {
        return ticketRepository.findById(TicketId.of(ticketId))
                .orElseThrow(() -> new TicketNotFoundException(ticketId));
    }

    public static class TicketNotFoundException extends RuntimeException {
        public TicketNotFoundException(String id) { super("Ticket not found: " + id); }
    }
}
