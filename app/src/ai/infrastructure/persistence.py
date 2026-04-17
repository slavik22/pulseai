from datetime import UTC
from uuid import UUID

import sqlalchemy as sa
from sqlalchemy import String, Text
from sqlalchemy.dialects.postgresql import ARRAY, UUID as PG_UUID
from sqlalchemy.ext.asyncio import AsyncSession

from src.ai.domain.model import (
    AiSuggestion,
    ConfidenceScore,
    KnowledgeArticle,
    SuggestionStatus,
)
from src.ai.domain.repository import AiSuggestionRepository, KnowledgeArticleRepository
from src.database import Base


class AiSuggestionORM(Base):
    __tablename__ = "ai_suggestions"

    id = sa.Column(PG_UUID(as_uuid=True), primary_key=True)
    ticket_id = sa.Column(String(255), nullable=False, index=True, unique=True)
    draft_response = sa.Column(Text, nullable=False)
    confidence_score = sa.Column(sa.Float, nullable=False)
    status = sa.Column(String(20), nullable=False, default="PENDING")
    source_article_ids = sa.Column(ARRAY(Text), nullable=False, server_default="{}")
    edited_response = sa.Column(Text, nullable=True)
    created_at = sa.Column(sa.DateTime(timezone=True), nullable=False)
    updated_at = sa.Column(sa.DateTime(timezone=True), nullable=False)


class KnowledgeArticleORM(Base):
    __tablename__ = "knowledge_articles"

    id = sa.Column(PG_UUID(as_uuid=True), primary_key=True)
    title = sa.Column(String(255), nullable=False)
    content = sa.Column(Text, nullable=False)
    category = sa.Column(String(100), nullable=True, index=True)
    is_verified_by_expert = sa.Column(sa.Boolean, nullable=False, default=False)
    is_indexed = sa.Column(sa.Boolean, nullable=False, default=False)
    created_at = sa.Column(sa.DateTime(timezone=True), nullable=False)
    updated_at = sa.Column(sa.DateTime(timezone=True), nullable=False)


# ── Mappers ───────────────────────────────────────────────────────────────────

def _suggestion_to_domain(row: AiSuggestionORM) -> AiSuggestion:
    return AiSuggestion(
        id=row.id,
        ticket_id=row.ticket_id,
        draft_response=row.draft_response,
        confidence=ConfidenceScore(value=row.confidence_score),
        status=SuggestionStatus(row.status),
        source_article_ids=list(row.source_article_ids or []),
        edited_response=row.edited_response,
        created_at=row.created_at.replace(tzinfo=UTC) if row.created_at.tzinfo is None else row.created_at,
        updated_at=row.updated_at.replace(tzinfo=UTC) if row.updated_at.tzinfo is None else row.updated_at,
    )


def _article_to_domain(row: KnowledgeArticleORM) -> KnowledgeArticle:
    return KnowledgeArticle(
        id=row.id,
        title=row.title,
        content=row.content,
        category=row.category,
        is_verified_by_expert=row.is_verified_by_expert,
        is_indexed=row.is_indexed,
        created_at=row.created_at.replace(tzinfo=UTC) if row.created_at.tzinfo is None else row.created_at,
        updated_at=row.updated_at.replace(tzinfo=UTC) if row.updated_at.tzinfo is None else row.updated_at,
    )


# ── Repositories ──────────────────────────────────────────────────────────────

class SqlAlchemyAiSuggestionRepository(AiSuggestionRepository):
    def __init__(self, session: AsyncSession) -> None:
        self._session = session

    async def save(self, suggestion: AiSuggestion) -> AiSuggestion:
        existing = await self._session.get(AiSuggestionORM, suggestion.id)
        if existing is None:
            row = AiSuggestionORM(
                id=suggestion.id,
                ticket_id=suggestion.ticket_id,
                draft_response=suggestion.draft_response,
                confidence_score=suggestion.confidence.value,
                status=suggestion.status.value,
                source_article_ids=suggestion.source_article_ids,
                edited_response=suggestion.edited_response,
                created_at=suggestion.created_at,
                updated_at=suggestion.updated_at,
            )
            self._session.add(row)
        else:
            existing.status = suggestion.status.value
            existing.edited_response = suggestion.edited_response
            existing.updated_at = suggestion.updated_at
        await self._session.flush()
        return suggestion

    async def find_by_id(self, suggestion_id: UUID) -> AiSuggestion | None:
        row = await self._session.get(AiSuggestionORM, suggestion_id)
        return _suggestion_to_domain(row) if row else None

    async def find_by_ticket_id(self, ticket_id: str) -> AiSuggestion | None:
        result = await self._session.execute(
            sa.select(AiSuggestionORM).where(AiSuggestionORM.ticket_id == ticket_id)
        )
        row = result.scalars().first()
        return _suggestion_to_domain(row) if row else None


class SqlAlchemyKnowledgeArticleRepository(KnowledgeArticleRepository):
    def __init__(self, session: AsyncSession) -> None:
        self._session = session

    async def save(self, article: KnowledgeArticle) -> KnowledgeArticle:
        existing = await self._session.get(KnowledgeArticleORM, article.id)
        if existing is None:
            row = KnowledgeArticleORM(
                id=article.id,
                title=article.title,
                content=article.content,
                category=article.category,
                is_verified_by_expert=article.is_verified_by_expert,
                is_indexed=article.is_indexed,
                created_at=article.created_at,
                updated_at=article.updated_at,
            )
            self._session.add(row)
        else:
            existing.title = article.title
            existing.content = article.content
            existing.category = article.category
            existing.is_verified_by_expert = article.is_verified_by_expert
            existing.is_indexed = article.is_indexed
            existing.updated_at = article.updated_at
        await self._session.flush()
        return article

    async def find_by_id(self, article_id: UUID) -> KnowledgeArticle | None:
        row = await self._session.get(KnowledgeArticleORM, article_id)
        return _article_to_domain(row) if row else None

    async def find_all(self) -> list[KnowledgeArticle]:
        result = await self._session.execute(
            sa.select(KnowledgeArticleORM).order_by(KnowledgeArticleORM.created_at.desc())
        )
        return [_article_to_domain(r) for r in result.scalars().all()]

    async def delete(self, article_id: UUID) -> None:
        row = await self._session.get(KnowledgeArticleORM, article_id)
        if row:
            await self._session.delete(row)
