package com.pulseai.ticket.application.port.in;

import com.pulseai.ticket.application.dto.ResolveTicketCommand;
import com.pulseai.ticket.application.dto.TicketResponse;

public interface ResolveTicketUseCase {
    TicketResponse resolveTicket(ResolveTicketCommand command);
}
