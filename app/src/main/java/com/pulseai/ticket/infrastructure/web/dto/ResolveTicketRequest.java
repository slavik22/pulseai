package com.pulseai.ticket.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

public record ResolveTicketRequest(@NotBlank String resolutionNote) {}
