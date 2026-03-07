package com.pulseai.ticket.application.port.in;

import com.pulseai.ticket.application.dto.AssignTicketCommand;
import com.pulseai.ticket.application.dto.TicketResponse;

public interface AssignTicketUseCase {
    TicketResponse assignTicket(AssignTicketCommand command);
}
