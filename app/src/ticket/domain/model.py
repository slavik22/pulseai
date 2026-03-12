"""
Ticket aggregate — pure Python domain model with no framework dependencies.

State transitions are enforced here so invalid moves raise immediately
rather than being caught at the persistence layer.
"""
from __future__ import annotations

import uuid
from dataclasses import dataclass, field
from datetime import UTC, datetime
from enum import StrEnum
from typing import Any


class Priority(StrEnum):
    LOW = "LOW"
    MEDIUM = "MEDIUM"
    HIGH = "HIGH"
    CRITICAL = "CRITICAL"


class TicketStatus(StrEnum):
    OPEN = "OPEN"
    IN_PROGRESS = "IN_PROGRESS"
    RESOLVED = "RESOLVED"
    CLOSED = "CLOSED"

    _VALID_TRANSITIONS: dict[str, set[str]]  # annotated below

    def transition_to(self, target: "TicketStatus") -> "TicketStatus":
        allowed = _TRANSITIONS.get(self, set())
        if target not in allowed:
            raise ValueError(f"Invalid transition: {self} → {target}")
        return target


_TRANSITIONS: dict[TicketStatus, set[TicketStatus]] = {
    TicketStatus.OPEN: {TicketStatus.IN_PROGRESS, TicketStatus.CLOSED},
    TicketStatus.IN_PROGRESS: {TicketStatus.RESOLVED, TicketStatus.CLOSED},
    TicketStatus.RESOLVED: {TicketStatus.CLOSED},
    TicketStatus.CLOSED: set(),
}


@dataclass
class Ticket:
    id: uuid.UUID
    title: str
    content: str
    status: TicketStatus
    priority: Priority
    customer_id: str
    category: str | None
    assigned_agent_id: str | None
    assigned_agent_name: str | None
    resolution_note: str | None
    created_at: datetime
    updated_at: datetime
    resolved_at: datetime | None
    _domain_events: list[Any] = field(default_factory=list, repr=False, compare=False)

    # ── Factory ───────────────────────────────────────────────────────────────

    @classmethod
    def open(
        cls,
        title: str,
        content: str,
        priority: Priority,
        customer_id: str,
        category: str | None = None,
    ) -> "Ticket":
        _require_non_blank(title, "title")
        _require_non_blank(content, "content")
        _require_non_blank(customer_id, "customer_id")

        from src.shared.events import TicketCreatedEvent

        now = datetime.now(UTC)
        ticket = cls(
            id=uuid.uuid4(),
            title=title,
            content=content,
            status=TicketStatus.OPEN,
            priority=priority,
            customer_id=customer_id,
            category=category,
            assigned_agent_id=None,
            assigned_agent_name=None,
            resolution_note=None,
            created_at=now,
            updated_at=now,
            resolved_at=None,
        )
        ticket._domain_events.append(
            TicketCreatedEvent(
                ticket_id=str(ticket.id),
                title=title,
                content=content,
                priority=priority.value,
                customer_id=customer_id,
                category=category,
            )
        )
        return ticket

    # ── Commands ──────────────────────────────────────────────────────────────

    def assign_to(self, agent_id: str, agent_name: str) -> None:
        from src.shared.events import TicketAssignedEvent

        _require_non_blank(agent_id, "agent_id")
        _require_non_blank(agent_name, "agent_name")
        self.status = self.status.transition_to(TicketStatus.IN_PROGRESS)
        self.assigned_agent_id = agent_id
        self.assigned_agent_name = agent_name
        self.updated_at = datetime.now(UTC)
        self._domain_events.append(
            TicketAssignedEvent(ticket_id=str(self.id), agent_id=agent_id, agent_name=agent_name)
        )

    def resolve(self, resolution_note: str) -> None:
        from src.shared.events import TicketResolvedEvent

        _require_non_blank(resolution_note, "resolution_note")
        if not self.assigned_agent_id:
            raise ValueError("Cannot resolve an unassigned ticket")
        self.status = self.status.transition_to(TicketStatus.RESOLVED)
        self.resolution_note = resolution_note
        self.resolved_at = datetime.now(UTC)
        self.updated_at = self.resolved_at
        minutes = int((self.resolved_at - self.created_at).total_seconds() / 60)
        self._domain_events.append(
            TicketResolvedEvent(
                ticket_id=str(self.id),
                agent_id=self.assigned_agent_id,
                resolution_note=resolution_note,
                resolution_minutes=minutes,
            )
        )

    def pull_domain_events(self) -> list[Any]:
        events, self._domain_events = self._domain_events, []
        return events


def _require_non_blank(value: str | None, field: str) -> None:
    if not value or not value.strip():
        raise ValueError(f"{field} must not be blank")
