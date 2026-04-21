import logging
from uuid import UUID

from src.ai.application.dto import (
    CreateArticleRequest,
    KnowledgeArticleResponse,
    SuggestionResponse,
)
from src.ai.domain.model import AiSuggestion, KnowledgeArticle
from src.ai.domain.rag_port import RagPort
from src.ai.domain.repository import AiSuggestionRepository, KnowledgeArticleRepository
from src.shared import metrics

log = logging.getLogger(__name__)


class SuggestionNotFoundError(Exception):
    def __init__(self, ref: str | UUID) -> None:
        super().__init__(f"AI suggestion not found: {ref}")


class AiApplicationService:
    def __init__(
        self,
        rag: RagPort,
        suggestion_repo: AiSuggestionRepository,
        article_repo: KnowledgeArticleRepository,
    ) -> None:
        self._rag = rag
        self._suggestion_repo = suggestion_repo
        self._article_repo = article_repo

    async def generate_suggestion(
        self,
        ticket_id: str,
        ticket_content: str,
        category: str | None,
    ) -> SuggestionResponse:
        log.info("Generating RAG suggestion for ticket %s", ticket_id)
        suggestion = await self._rag.generate_suggestion(ticket_id, ticket_content, category)
        saved = await self._suggestion_repo.save(suggestion)
        return SuggestionResponse.from_domain(saved)

    async def get_suggestion_by_ticket_id(self, ticket_id: str) -> SuggestionResponse:
        suggestion = await self._suggestion_repo.find_by_ticket_id(ticket_id)
        if suggestion is None:
            raise SuggestionNotFoundError(ticket_id)
        return SuggestionResponse.from_domain(suggestion)

    async def accept_suggestion(self, suggestion_id: UUID) -> SuggestionResponse:
        s = await self._load_or_raise(suggestion_id)
        s.accept()
        metrics.ai_suggestions_accepted.inc()
        saved = await self._suggestion_repo.save(s)
        return SuggestionResponse.from_domain(saved)

    async def reject_suggestion(self, suggestion_id: UUID) -> SuggestionResponse:
        s = await self._load_or_raise(suggestion_id)
        s.reject()
        metrics.ai_suggestions_rejected.inc()
        saved = await self._suggestion_repo.save(s)
        return SuggestionResponse.from_domain(saved)

    async def edit_suggestion(self, suggestion_id: UUID, edited_response: str) -> SuggestionResponse:
        s = await self._load_or_raise(suggestion_id)
        s.edit_and_accept(edited_response)
        metrics.ai_suggestions_edited.inc()
        saved = await self._suggestion_repo.save(s)
        return SuggestionResponse.from_domain(saved)

    async def get_all_articles(self) -> list[KnowledgeArticleResponse]:
        articles = await self._article_repo.find_all()
        return [KnowledgeArticleResponse.from_domain(a) for a in articles]

    async def create_and_index_article(self, request: CreateArticleRequest) -> KnowledgeArticleResponse:
        article = KnowledgeArticle.create(
            title=request.title,
            content=request.content,
            category=request.category,
        )
        saved = await self._article_repo.save(article)
        try:
            await self._rag.index_article(saved)
            saved.mark_indexed()
            await self._article_repo.save(saved)
            log.info("Created and indexed article %s", saved.id)
        except Exception as exc:
            log.warning(
                "Article %s saved but indexing failed (RAG unavailable): %s",
                saved.id,
                exc,
            )
        return KnowledgeArticleResponse.from_domain(saved)

    async def delete_article(self, article_id: UUID) -> None:
        await self._rag.remove_article_embeddings(str(article_id))
        await self._article_repo.delete(article_id)

    async def _load_or_raise(self, suggestion_id: UUID) -> AiSuggestion:
        suggestion = await self._suggestion_repo.find_by_id(suggestion_id)
        if suggestion is None:
            raise SuggestionNotFoundError(suggestion_id)
        return suggestion
