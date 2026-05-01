"""
Cross-module event routing — wires ticket domain events to the AI and
notification modules.

Registered at startup via event_bus.subscribe().  Each handler runs as a
background asyncio Task (fire-and-forget) so ticket HTTP responses are
never delayed by AI generation or WebSocket broadcast latency.

This replicates the behaviour of Spring's:
  @Async + @TransactionalEventListener(phase = AFTER_COMMIT)
"""
import logging
from collections.abc import Callable
from contextlib import asynccontextmanager

from sqlalchemy.ext.asyncio import AsyncSession

from src.ai.application.service import AiApplicationService
from src.ai.domain.rag_port import RagPort
from src.ai.infrastructure.persistence import (
    SqlAlchemyAiSuggestionRepository,
    SqlAlchemyKnowledgeArticleRepository,
)
from src.notification.service import NotificationService
from src.shared.events import (
    TicketAssignedEvent,
    TicketCreatedEvent,
    TicketResolvedEvent,
    event_bus,
)

log = logging.getLogger(__name__)

SessionFactory = Callable[[], AsyncSession]


def register_event_handlers(
    session_factory: SessionFactory,
    rag_adapter: RagPort,
    notification_service: NotificationService,
) -> None:
    """
    Called once at application startup to wire handlers into the event bus.

    Each handler opens its own short-lived DB session so it is fully decoupled
    from the request session that already committed the ticket.
    """

    async def on_ticket_created(event: TicketCreatedEvent) -> None:
        log.info("Processing TicketCreatedEvent for ticket %s", event.ticket_id)
        content = (
            f"Title: {event.title}\n"
            f"Category: {event.category}\n"
            f"Priority: {event.priority}\n\n"
            f"Customer Message:\n{event.content}"
        )
        async with session_factory() as session:
            ai_service = AiApplicationService(
                rag=rag_adapter,
                suggestion_repo=SqlAlchemyAiSuggestionRepository(session),
                article_repo=SqlAlchemyKnowledgeArticleRepository(session),
            )
            try:
                await ai_service.generate_suggestion(event.ticket_id, content, event.category)
                await session.commit()
            except Exception:
                await session.rollback()
                log.exception("Failed to generate AI suggestion for ticket %s", event.ticket_id)

        await notification_service.notify_ticket_created(event)

    async def on_ticket_assigned(event: TicketAssignedEvent) -> None:
        await notification_service.notify_ticket_assigned(event)

    async def on_ticket_resolved(event: TicketResolvedEvent) -> None:
        await notification_service.notify_ticket_resolved(event)

    event_bus.subscribe(TicketCreatedEvent, on_ticket_created)
    event_bus.subscribe(TicketAssignedEvent, on_ticket_assigned)
    event_bus.subscribe(TicketResolvedEvent, on_ticket_resolved)

    log.info("Event handlers registered")
