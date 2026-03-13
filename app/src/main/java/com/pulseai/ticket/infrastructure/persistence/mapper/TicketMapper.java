package com.pulseai.ticket.infrastructure.persistence.mapper;

import com.pulseai.ticket.domain.model.Ticket;
import com.pulseai.ticket.domain.model.TicketId;
import com.pulseai.ticket.infrastructure.persistence.entity.TicketEntity;
import org.springframework.stereotype.Component;

@Component
public class TicketMapper {

    public TicketEntity toEntity(Ticket ticket) {
        return TicketEntity.builder()
                .id(ticket.getId().value())
                .title(ticket.getTitle())
                .content(ticket.getContent())
                .status(ticket.getStatus())
                .priority(ticket.getPriority())
                .customerId(ticket.getCustomerId())
                .category(ticket.getCategory())
                .assignedAgentId(ticket.getAssignedAgentId())
                .assignedAgentName(ticket.getAssignedAgentName())
                .resolutionNote(ticket.getResolutionNote())
                .resolvedAt(ticket.getResolvedAt())
                .build();
    }

    public void updateEntity(TicketEntity entity, Ticket ticket) {
        entity.setTitle(ticket.getTitle());
        entity.setContent(ticket.getContent());
        entity.setStatus(ticket.getStatus());
        entity.setPriority(ticket.getPriority());
        entity.setCategory(ticket.getCategory());
        entity.setAssignedAgentId(ticket.getAssignedAgentId());
        entity.setAssignedAgentName(ticket.getAssignedAgentName());
        entity.setResolutionNote(ticket.getResolutionNote());
        entity.setResolvedAt(ticket.getResolvedAt());
    }

    public Ticket toDomain(TicketEntity e) {
        return Ticket.reconstitute(
                TicketId.of(e.getId()), e.getTitle(), e.getContent(),
                e.getStatus(), e.getPriority(), e.getCustomerId(), e.getCategory(),
                e.getAssignedAgentId(), e.getAssignedAgentName(), e.getResolutionNote(),
                e.getCreatedAt(), e.getUpdatedAt(), e.getResolvedAt()
        );
    }
}
