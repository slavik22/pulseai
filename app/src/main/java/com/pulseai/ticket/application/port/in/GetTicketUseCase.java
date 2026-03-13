package com.pulseai.ticket.application.port.in;

import com.pulseai.ticket.application.dto.TicketResponse;

import java.util.List;

public interface GetTicketUseCase {
    TicketResponse getTicketById(String ticketId);
    List<TicketResponse> getTicketsByCustomer(String customerId);
    List<TicketResponse> getTicketsByAgent(String agentId);
}
