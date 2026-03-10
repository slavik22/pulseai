package com.pulseai.ticket.infrastructure.persistence;

import com.pulseai.ticket.domain.model.TicketStatus;
import com.pulseai.ticket.infrastructure.persistence.entity.TicketEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TicketJpaRepository extends JpaRepository<TicketEntity, UUID> {
    List<TicketEntity> findByCustomerId(String customerId);
    List<TicketEntity> findByStatus(TicketStatus status);
    List<TicketEntity> findByAssignedAgentId(String agentId);
}
