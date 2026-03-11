package com.pulseai.ticket.infrastructure.web.dto;

import com.pulseai.ticket.domain.model.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTicketRequest(
        @NotBlank String title,
        @NotBlank String content,
        @NotNull Priority priority,
        @NotBlank String customerId,
        String category
) {}
