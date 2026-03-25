package com.pulseai.event;

import com.pulseai.ai.application.service.AiApplicationService;
import com.pulseai.notification.application.service.NotificationService;
import com.pulseai.ticket.domain.model.events.TicketAssignedEvent;
import com.pulseai.ticket.domain.model.events.TicketCreatedEvent;
import com.pulseai.ticket.domain.model.events.TicketResolvedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Cross-module event routing — the glue between ticket domain events and
 * the AI and notification modules.
 *
 * WHY @TransactionalEventListener(phase = AFTER_COMMIT)?
 * Events are published inside a @Transactional method. If we reacted
 * BEFORE_COMMIT, a DB rollback would leave orphan AI suggestions.
 * AFTER_COMMIT means: the ticket is already in the DB — safe to act.
 *
 * WHY @Async?
 * AI suggestion generation calls OpenAI — can take 3–10 seconds.
 * Without @Async, the HTTP response for "create ticket" would block for that
 * entire duration. With @Async, the response returns in <50ms; the AI
 * generation happens in a background thread.
 */
@Component
public class TicketEventListener {

    private static final Logger log = LoggerFactory.getLogger(TicketEventListener.class);

    private final AiApplicationService aiService;
    private final NotificationService notificationService;

    public TicketEventListener(AiApplicationService aiService,
                                NotificationService notificationService) {
        this.aiService = aiService;
        this.notificationService = notificationService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTicketCreated(TicketCreatedEvent event) {
        log.info("Processing TicketCreatedEvent for ticket {}", event.ticketId());

        // Generate AI suggestion in background — doesn't block the HTTP response
        String content = "Title: %s\nCategory: %s\nPriority: %s\n\nCustomer Message:\n%s".formatted(
                event.title(), event.category(), event.priority(), event.content());
        try {
            aiService.generateSuggestion(event.ticketId().toString(), content, event.category());
        } catch (Exception e) {
            log.error("Failed to generate AI suggestion for ticket {}: {}", event.ticketId(), e.getMessage());
        }

        notificationService.notifyTicketCreated(event);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTicketAssigned(TicketAssignedEvent event) {
        notificationService.notifyTicketAssigned(event);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTicketResolved(TicketResolvedEvent event) {
        notificationService.notifyTicketResolved(event);
    }
}
