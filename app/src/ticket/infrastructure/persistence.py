from datetime import UTC, datetime
from uuid import UUID

import sqlalchemy as sa
from sqlalchemy import String, text
from sqlalchemy.dialects.postgresql import UUID as PG_UUID
from sqlalchemy.ext.asyncio import AsyncSession

from src.database import Base
from src.ticket.domain.model import Priority, Ticket, TicketStatus
from src.ticket.domain.repository import TicketRepository


class TicketORM(Base):
    __tablename__ = "tickets"

    id = sa.Column(PG_UUID(as_uuid=True), primary_key=True)
    title = sa.Column(String(255), nullable=False)
    content = sa.Column(sa.Text, nullable=False)
    status = sa.Column(String(20), nullable=False, default="OPEN", index=True)
    priority = sa.Column(String(20), nullable=False)
    customer_id = sa.Column(String(255), nullable=False, index=True)
    category = sa.Column(String(100), nullable=True, index=True)
    assigned_agent_id = sa.Column(String(255), nullable=True, index=True)
    assigned_agent_name = sa.Column(String(255), nullable=True)
    resolution_note = sa.Column(sa.Text, nullable=True)
    created_at = sa.Column(sa.DateTime(timezone=True), nullable=False)
    updated_at = sa.Column(sa.DateTime(timezone=True), nullable=False)
    resolved_at = sa.Column(sa.DateTime(timezone=True), nullable=True)


def _to_domain(row: TicketORM) -> Ticket:
    return Ticket(
        id=row.id,
        title=row.title,
        content=row.content,
        status=TicketStatus(row.status),
        priority=Priority(row.priority),
        customer_id=row.customer_id,
        category=row.category,
        assigned_agent_id=row.assigned_agent_id,
        assigned_agent_name=row.assigned_agent_name,
        resolution_note=row.resolution_note,
        created_at=row.created_at.replace(tzinfo=UTC) if row.created_at.tzinfo is None else row.created_at,
        updated_at=row.updated_at.replace(tzinfo=UTC) if row.updated_at.tzinfo is None else row.updated_at,
        resolved_at=(
            row.resolved_at.replace(tzinfo=UTC)
            if row.resolved_at and row.resolved_at.tzinfo is None
            else row.resolved_at
        ),
    )


def _to_orm(ticket: Ticket) -> TicketORM:
    return TicketORM(
        id=ticket.id,
        title=ticket.title,
        content=ticket.content,
        status=ticket.status.value,
        priority=ticket.priority.value,
        customer_id=ticket.customer_id,
        category=ticket.category,
        assigned_agent_id=ticket.assigned_agent_id,
        assigned_agent_name=ticket.assigned_agent_name,
        resolution_note=ticket.resolution_note,
        created_at=ticket.created_at,
        updated_at=ticket.updated_at,
        resolved_at=ticket.resolved_at,
    )


class SqlAlchemyTicketRepository(TicketRepository):
    def __init__(self, session: AsyncSession) -> None:
        self._session = session

    async def save(self, ticket: Ticket) -> Ticket:
        existing = await self._session.get(TicketORM, ticket.id)
        if existing is None:
            row = _to_orm(ticket)
            self._session.add(row)
        else:
            existing.title = ticket.title
            existing.content = ticket.content
            existing.status = ticket.status.value
            existing.priority = ticket.priority.value
            existing.assigned_agent_id = ticket.assigned_agent_id
            existing.assigned_agent_name = ticket.assigned_agent_name
            existing.resolution_note = ticket.resolution_note
            existing.updated_at = ticket.updated_at
            existing.resolved_at = ticket.resolved_at
        await self._session.flush()
        return ticket

    async def find_by_id(self, ticket_id: UUID) -> Ticket | None:
        row = await self._session.get(TicketORM, ticket_id)
        return _to_domain(row) if row else None

    async def find_by_customer_id(self, customer_id: str) -> list[Ticket]:
        result = await self._session.execute(
            sa.select(TicketORM).where(TicketORM.customer_id == customer_id)
        )
        return [_to_domain(r) for r in result.scalars().all()]

    async def find_by_agent_id(self, agent_id: str) -> list[Ticket]:
        result = await self._session.execute(
            sa.select(TicketORM).where(TicketORM.assigned_agent_id == agent_id)
        )
        return [_to_domain(r) for r in result.scalars().all()]

    async def find_all(self) -> list[Ticket]:
        result = await self._session.execute(
            sa.select(TicketORM).order_by(TicketORM.created_at.desc())
        )
        return [_to_domain(r) for r in result.scalars().all()]
