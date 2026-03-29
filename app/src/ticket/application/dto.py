from datetime import datetime
from uuid import UUID

from pydantic import BaseModel, Field

from src.ticket.domain.model import Priority, Ticket, TicketStatus


class CreateTicketRequest(BaseModel):
    title: str = Field(min_length=1, max_length=255)
    content: str = Field(min_length=1)
    priority: Priority
    customer_id: str = Field(min_length=1, alias="customerId")
    category: str | None = None

    model_config = {"populate_by_name": True}


class AssignTicketRequest(BaseModel):
    agent_id: str = Field(min_length=1, alias="agentId")
    agent_name: str = Field(min_length=1, alias="agentName")

    model_config = {"populate_by_name": True}


class ResolveTicketRequest(BaseModel):
    resolution_note: str = Field(min_length=1, alias="resolutionNote")

    model_config = {"populate_by_name": True}


class TicketResponse(BaseModel):
    id: UUID
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

    model_config = {"from_attributes": True}

    @classmethod
    def from_domain(cls, ticket: Ticket) -> "TicketResponse":
        return cls(
            id=ticket.id,
            title=ticket.title,
            content=ticket.content,
            status=ticket.status,
            priority=ticket.priority,
            customer_id=ticket.customer_id,
            category=ticket.category,
            assigned_agent_id=ticket.assigned_agent_id,
            assigned_agent_name=ticket.assigned_agent_name,
            resolution_note=ticket.resolution_note,
            created_at=ticket.created_at,
            updated_at=ticket.updated_at,
            resolved_at=ticket.resolved_at,
        )
