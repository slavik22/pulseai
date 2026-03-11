package com.pulseai.ticket.infrastructure.web;

import com.pulseai.ticket.application.dto.*;
import com.pulseai.ticket.application.port.in.*;
import com.pulseai.ticket.infrastructure.web.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {

    private final CreateTicketUseCase createTicketUseCase;
    private final AssignTicketUseCase assignTicketUseCase;
    private final ResolveTicketUseCase resolveTicketUseCase;
    private final GetTicketUseCase getTicketUseCase;

    public TicketController(CreateTicketUseCase createTicketUseCase,
                            AssignTicketUseCase assignTicketUseCase,
                            ResolveTicketUseCase resolveTicketUseCase,
                            GetTicketUseCase getTicketUseCase) {
        this.createTicketUseCase = createTicketUseCase;
        this.assignTicketUseCase = assignTicketUseCase;
        this.resolveTicketUseCase = resolveTicketUseCase;
        this.getTicketUseCase = getTicketUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponse createTicket(@Valid @RequestBody CreateTicketRequest request) {
        return createTicketUseCase.createTicket(new CreateTicketCommand(
                request.title(), request.content(), request.priority(),
                request.customerId(), request.category()));
    }

    @GetMapping("/{ticketId}")
    public TicketResponse getTicket(@PathVariable String ticketId) {
        return getTicketUseCase.getTicketById(ticketId);
    }

    @GetMapping
    public List<TicketResponse> getByCustomer(@RequestParam String customerId) {
        return getTicketUseCase.getTicketsByCustomer(customerId);
    }

    @GetMapping("/agent/{agentId}")
    public List<TicketResponse> getByAgent(@PathVariable String agentId) {
        return getTicketUseCase.getTicketsByAgent(agentId);
    }

    @PatchMapping("/{ticketId}/assign")
    public TicketResponse assign(@PathVariable String ticketId,
                                 @Valid @RequestBody AssignTicketRequest request) {
        return assignTicketUseCase.assignTicket(
                new AssignTicketCommand(ticketId, request.agentId(), request.agentName()));
    }

    @PatchMapping("/{ticketId}/resolve")
    public TicketResponse resolve(@PathVariable String ticketId,
                                  @Valid @RequestBody ResolveTicketRequest request) {
        return resolveTicketUseCase.resolveTicket(
                new ResolveTicketCommand(ticketId, request.resolutionNote()));
    }
}
