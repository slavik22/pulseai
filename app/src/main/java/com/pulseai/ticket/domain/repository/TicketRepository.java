package com.pulseai.ticket.domain.repository;

import com.pulseai.ticket.domain.model.Ticket;
import com.pulseai.ticket.domain.model.TicketId;
import com.pulseai.ticket.domain.model.TicketStatus;

import java.util.List;
import java.util.Optional;

public interface TicketRepository {
    Ticket save(Ticket ticket);
    Optional<Ticket> findById(TicketId id);
    List<Ticket> findByCustomerId(String customerId);
    List<Ticket> findByStatus(TicketStatus status);
    List<Ticket> findByAssignedAgentId(String agentId);
}
