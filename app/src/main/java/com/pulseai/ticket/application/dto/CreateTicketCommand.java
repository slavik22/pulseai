package com.pulseai.ticket.application.dto;

import com.pulseai.ticket.domain.model.Priority;

public record CreateTicketCommand(
        String title,
        String content,
        Priority priority,
        String customerId,
        String category
) {}
