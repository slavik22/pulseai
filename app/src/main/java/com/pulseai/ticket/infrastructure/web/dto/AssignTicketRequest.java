package com.pulseai.ticket.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

public record AssignTicketRequest(@NotBlank String agentId, @NotBlank String agentName) {}
