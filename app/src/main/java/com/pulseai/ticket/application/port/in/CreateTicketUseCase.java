package com.pulseai.ticket.application.port.in;

import com.pulseai.ticket.application.dto.CreateTicketCommand;
import com.pulseai.ticket.application.dto.TicketResponse;

public interface CreateTicketUseCase {
    TicketResponse createTicket(CreateTicketCommand command);
}
