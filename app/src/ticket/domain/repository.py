from abc import ABC, abstractmethod
from uuid import UUID

from src.ticket.domain.model import Ticket


class TicketRepository(ABC):
    @abstractmethod
    async def save(self, ticket: Ticket) -> Ticket: ...

    @abstractmethod
    async def find_by_id(self, ticket_id: UUID) -> Ticket | None: ...

    @abstractmethod
    async def find_by_customer_id(self, customer_id: str) -> list[Ticket]: ...

    @abstractmethod
    async def find_by_agent_id(self, agent_id: str) -> list[Ticket]: ...

    @abstractmethod
    async def find_all(self) -> list[Ticket]: ...
