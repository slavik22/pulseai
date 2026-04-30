package com.pulseai.notification.application.service;

import com.pulseai.ticket.domain.model.events.TicketAssignedEvent;
import com.pulseai.ticket.domain.model.events.TicketCreatedEvent;
import com.pulseai.ticket.domain.model.events.TicketResolvedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Sends real-time notifications to connected frontend clients via WebSocket/STOMP.
 *
 * In a monolith, this is a direct method call — no Kafka, no Redis pub/sub.
 * For multi-instance deployments, replace enableSimpleBroker() with a full
 * STOMP broker (RabbitMQ) which handles cross-instance message routing natively.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final String TOPIC = "/topic/notifications";

    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void notifyTicketCreated(TicketCreatedEvent event) {
        send(Map.of(
                "type", "TICKET_CREATED",
                "ticketId", event.ticketId().toString(),
                "title", event.title(),
                "priority", event.priority().name()
        ));
        log.debug("Notified TICKET_CREATED for {}", event.ticketId());
    }

    public void notifyTicketAssigned(TicketAssignedEvent event) {
        send(Map.of(
                "type", "TICKET_ASSIGNED",
                "ticketId", event.ticketId().toString(),
                "agentId", event.agentId(),
                "agentName", event.agentName()
        ));
    }

    public void notifyTicketResolved(TicketResolvedEvent event) {
        send(Map.of(
                "type", "TICKET_RESOLVED",
                "ticketId", event.ticketId().toString(),
                "resolutionMinutes", String.valueOf(event.resolutionMinutes())
        ));
    }

    private void send(Object payload) {
        messagingTemplate.convertAndSend(TOPIC, payload);
    }
}
