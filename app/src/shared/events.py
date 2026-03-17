"""
In-process async event bus.

Mirrors the behaviour of Spring's ApplicationEventPublisher +
@TransactionalEventListener(AFTER_COMMIT) + @Async:

  - Events are dispatched AFTER the DB transaction commits (the service
    calls publish() once save() is done and the session is committed).
  - Each handler runs in a separate asyncio Task so the HTTP response
    is never blocked by slow AI/notification work.
"""
from __future__ import annotations

import asyncio
import logging
from collections import defaultdict
from collections.abc import Awaitable, Callable
from dataclasses import dataclass, field
from typing import Any, TypeVar

log = logging.getLogger(__name__)

Handler = Callable[[Any], Awaitable[None]]
T = TypeVar("T")


class EventBus:
    def __init__(self) -> None:
        self._handlers: dict[type, list[Handler]] = defaultdict(list)

    def subscribe(self, event_type: type[T], handler: Callable[[T], Awaitable[None]]) -> None:
        self._handlers[event_type].append(handler)  # type: ignore[arg-type]

    async def publish(self, event: Any) -> None:
        handlers = self._handlers.get(type(event), [])
        for handler in handlers:
            asyncio.create_task(self._safe_handle(handler, event))

    @staticmethod
    async def _safe_handle(handler: Handler, event: Any) -> None:
        try:
            await handler(event)
        except Exception:
            log.exception("Event handler %s raised for event %s", handler.__name__, type(event).__name__)


# ── Domain events ────────────────────────────────────────────────────────────

@dataclass(frozen=True)
class TicketCreatedEvent:
    ticket_id: str
    title: str
    content: str
    priority: str
    customer_id: str
    category: str | None


@dataclass(frozen=True)
class TicketAssignedEvent:
    ticket_id: str
    agent_id: str
    agent_name: str


@dataclass(frozen=True)
class TicketResolvedEvent:
    ticket_id: str
    agent_id: str
    resolution_note: str
    resolution_minutes: int


# Singleton used throughout the app
event_bus = EventBus()
