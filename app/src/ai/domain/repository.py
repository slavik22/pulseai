from abc import ABC, abstractmethod
from uuid import UUID

from src.ai.domain.model import AiSuggestion, KnowledgeArticle


class AiSuggestionRepository(ABC):
    @abstractmethod
    async def save(self, suggestion: AiSuggestion) -> AiSuggestion: ...

    @abstractmethod
    async def find_by_id(self, suggestion_id: UUID) -> AiSuggestion | None: ...

    @abstractmethod
    async def find_by_ticket_id(self, ticket_id: str) -> AiSuggestion | None: ...


class KnowledgeArticleRepository(ABC):
    @abstractmethod
    async def save(self, article: KnowledgeArticle) -> KnowledgeArticle: ...

    @abstractmethod
    async def find_by_id(self, article_id: UUID) -> KnowledgeArticle | None: ...

    @abstractmethod
    async def find_all(self) -> list[KnowledgeArticle]: ...

    @abstractmethod
    async def delete(self, article_id: UUID) -> None: ...
