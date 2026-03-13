package com.pulseai.ticket.infrastructure.persistence;

import com.pulseai.ticket.domain.model.Ticket;
import com.pulseai.ticket.domain.model.TicketId;
import com.pulseai.ticket.domain.model.TicketStatus;
import com.pulseai.ticket.domain.repository.TicketRepository;
import com.pulseai.ticket.infrastructure.persistence.entity.TicketEntity;
import com.pulseai.ticket.infrastructure.persistence.mapper.TicketMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class TicketRepositoryAdapter implements TicketRepository {

    private final TicketJpaRepository jpaRepository;
    private final TicketMapper mapper;

    public TicketRepositoryAdapter(TicketJpaRepository jpaRepository, TicketMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Ticket save(Ticket ticket) {
        // Load existing entity if present (preserves managed state and timestamps),
        // otherwise build a new one. This avoids version-detection issues with Spring Data.
        TicketEntity entity = jpaRepository.findById(ticket.getId().value())
                .map(existing -> { mapper.updateEntity(existing, ticket); return existing; })
                .orElseGet(() -> mapper.toEntity(ticket));

        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<Ticket> findById(TicketId id) {
        return jpaRepository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public List<Ticket> findByCustomerId(String customerId) {
        return jpaRepository.findByCustomerId(customerId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Ticket> findByStatus(TicketStatus status) {
        return jpaRepository.findByStatus(status).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Ticket> findByAssignedAgentId(String agentId) {
        return jpaRepository.findByAssignedAgentId(agentId).stream().map(mapper::toDomain).toList();
    }
}
