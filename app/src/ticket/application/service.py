"""
Ticket application service.

Orchestrates domain operations and publishes domain events AFTER the DB
session commits — replicating @TransactionalEventListener(AFTER_COMMIT).
The event_bus.publish() creates asyncio Tasks so downstream AI and
notification work never blocks the HTTP response.
"""
from uuid import UUID

from src.shared.events import event_bus
from src.shared import metrics
from src.ticket.application.dto import (
    AssignTicketRequest,
    CreateTicketRequest,
    ResolveTicketRequest,
    TicketResponse,
)
from src.ticket.domain.model import Priority, Ticket
from src.ticket.domain.repository import TicketRepository


class TicketNotFoundError(Exception):
    def __init__(self, ticket_id: str | UUID) -> None:
        super().__init__(f"Ticket not found: {ticket_id}")


class TicketApplicationService:
    def __init__(self, repo: TicketRepository) -> None:
        self._repo = repo

    async def create_ticket(self, request: CreateTicketRequest) -> TicketResponse:
        ticket = Ticket.open(
            title=request.title,
            content=request.content,
            priority=Priority(request.priority),
            customer_id=request.customer_id,
            category=request.category,
        )
        events = ticket.pull_domain_events()
        saved = await self._repo.save(ticket)

        # Publish AFTER commit — handlers run as background asyncio tasks
        for event in events:
            await event_bus.publish(event)

        metrics.tickets_created_total.inc()
        return TicketResponse.from_domain(saved)

    async def assign_ticket(self, ticket_id: UUID, request: AssignTicketRequest) -> TicketResponse:
        ticket = await self._load_or_raise(ticket_id)
        ticket.assign_to(agent_id=request.agent_id, agent_name=request.agent_name)
        events = ticket.pull_domain_events()
        saved = await self._repo.save(ticket)
        for event in events:
            await event_bus.publish(event)
        return TicketResponse.from_domain(saved)

    async def resolve_ticket(self, ticket_id: UUID, request: ResolveTicketRequest) -> TicketResponse:
        ticket = await self._load_or_raise(ticket_id)
        ticket.resolve(resolution_note=request.resolution_note)
        events = ticket.pull_domain_events()
        saved = await self._repo.save(ticket)
        for event in events:
            await event_bus.publish(event)
        metrics.tickets_resolved_total.inc()
        return TicketResponse.from_domain(saved)

    async def get_ticket_by_id(self, ticket_id: UUID) -> TicketResponse:
        return TicketResponse.from_domain(await self._load_or_raise(ticket_id))

    async def get_tickets_by_customer(self, customer_id: str) -> list[TicketResponse]:
        tickets = await self._repo.find_by_customer_id(customer_id)
        return [TicketResponse.from_domain(t) for t in tickets]

    async def get_tickets_by_agent(self, agent_id: str) -> list[TicketResponse]:
        tickets = await self._repo.find_by_agent_id(agent_id)
        return [TicketResponse.from_domain(t) for t in tickets]

    async def get_all_tickets(self) -> list[TicketResponse]:
        tickets = await self._repo.find_all()
        return [TicketResponse.from_domain(t) for t in tickets]

    async def _load_or_raise(self, ticket_id: UUID) -> Ticket:
        ticket = await self._repo.find_by_id(ticket_id)
        if ticket is None:
            raise TicketNotFoundError(ticket_id)
        return ticket
