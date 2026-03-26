from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession

from src.database import get_db
from src.ticket.application.dto import (
    AssignTicketRequest,
    CreateTicketRequest,
    ResolveTicketRequest,
    TicketResponse,
)
from src.ticket.application.service import TicketApplicationService, TicketNotFoundError
from src.ticket.infrastructure.persistence import SqlAlchemyTicketRepository

router = APIRouter(prefix="/tickets", tags=["tickets"])


def _get_service(db: AsyncSession = Depends(get_db)) -> TicketApplicationService:
    return TicketApplicationService(SqlAlchemyTicketRepository(db))


@router.post("", response_model=TicketResponse, status_code=status.HTTP_201_CREATED)
async def create_ticket(
    request: CreateTicketRequest,
    service: TicketApplicationService = Depends(_get_service),
) -> TicketResponse:
    return await service.create_ticket(request)


@router.get("", response_model=list[TicketResponse])
async def list_tickets(
    customer_id: str | None = None,
    agent_id: str | None = None,
    service: TicketApplicationService = Depends(_get_service),
) -> list[TicketResponse]:
    if customer_id:
        return await service.get_tickets_by_customer(customer_id)
    if agent_id:
        return await service.get_tickets_by_agent(agent_id)
    return await service.get_all_tickets()


@router.get("/{ticket_id}", response_model=TicketResponse)
async def get_ticket(
    ticket_id: UUID,
    service: TicketApplicationService = Depends(_get_service),
) -> TicketResponse:
    try:
        return await service.get_ticket_by_id(ticket_id)
    except TicketNotFoundError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(exc)) from exc


@router.patch("/{ticket_id}/assign", response_model=TicketResponse)
async def assign_ticket(
    ticket_id: UUID,
    request: AssignTicketRequest,
    service: TicketApplicationService = Depends(_get_service),
) -> TicketResponse:
    try:
        return await service.assign_ticket(ticket_id, request)
    except TicketNotFoundError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(exc)) from exc
    except ValueError as exc:
        raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY, detail=str(exc)) from exc


@router.patch("/{ticket_id}/resolve", response_model=TicketResponse)
async def resolve_ticket(
    ticket_id: UUID,
    request: ResolveTicketRequest,
    service: TicketApplicationService = Depends(_get_service),
) -> TicketResponse:
    try:
        return await service.resolve_ticket(ticket_id, request)
    except TicketNotFoundError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(exc)) from exc
    except ValueError as exc:
        raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY, detail=str(exc)) from exc
