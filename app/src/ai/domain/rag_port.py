from abc import ABC, abstractmethod

from src.ai.domain.model import AiSuggestion, KnowledgeArticle


class RagPort(ABC):
    """
    Outbound port that the AI application service uses to interact with the
    RAG pipeline. The concrete implementation lives in infrastructure/rag.py
    and depends on LangChain + OpenAI — keeping this interface in the domain
    keeps the domain framework-free.
    """

    @abstractmethod
    async def generate_suggestion(
        self,
        ticket_id: str,
        ticket_content: str,
        category: str | None,
    ) -> AiSuggestion: ...

    @abstractmethod
    async def index_article(self, article: KnowledgeArticle) -> None: ...

    @abstractmethod
    async def remove_article_embeddings(self, article_id: str) -> None: ...
