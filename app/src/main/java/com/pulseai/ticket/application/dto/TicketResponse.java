package com.pulseai.ticket.application.dto;

import com.pulseai.ticket.domain.model.Priority;
import com.pulseai.ticket.domain.model.Ticket;
import com.pulseai.ticket.domain.model.TicketStatus;

import java.time.Instant;

public record TicketResponse(
        String id,
        String title,
        String content,
        TicketStatus status,
        Priority priority,
        String customerId,
        String category,
        String assignedAgentId,
        String assignedAgentName,
        String resolutionNote,
        Instant createdAt,
        Instant updatedAt,
        Instant resolvedAt
) {
    public static TicketResponse from(Ticket ticket) {
        return new TicketResponse(
                ticket.getId().toString(),
                ticket.getTitle(),
                ticket.getContent(),
                ticket.getStatus(),
                ticket.getPriority(),
                ticket.getCustomerId(),
                ticket.getCategory(),
                ticket.getAssignedAgentId(),
                ticket.getAssignedAgentName(),
                ticket.getResolutionNote(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                ticket.getResolvedAt()
        );
    }
}
