from datetime import datetime
from uuid import UUID

from pydantic import BaseModel

from src.ai.domain.model import AiSuggestion, KnowledgeArticle, SuggestionStatus


class SuggestionResponse(BaseModel):
    id: UUID
    ticket_id: str
    draft_response: str
    confidence_score: float
    confidence_label: str
    status: SuggestionStatus
    source_article_ids: list[str]
    edited_response: str | None
    created_at: datetime
    updated_at: datetime

    model_config = {"from_attributes": True}

    @classmethod
    def from_domain(cls, s: AiSuggestion) -> "SuggestionResponse":
        return cls(
            id=s.id,
            ticket_id=s.ticket_id,
            draft_response=s.draft_response,
            confidence_score=s.confidence.value,
            confidence_label="HIGH" if s.confidence.value >= 0.80 else ("MEDIUM" if s.confidence.is_reliable else "LOW"),
            status=s.status,
            source_article_ids=s.source_article_ids,
            edited_response=s.edited_response,
            created_at=s.created_at,
            updated_at=s.updated_at,
        )


class KnowledgeArticleResponse(BaseModel):
    id: UUID
    title: str
    content: str
    category: str | None
    is_verified_by_expert: bool
    is_indexed: bool
    created_at: datetime
    updated_at: datetime

    @classmethod
    def from_domain(cls, a: KnowledgeArticle) -> "KnowledgeArticleResponse":
        return cls(
            id=a.id,
            title=a.title,
            content=a.content,
            category=a.category,
            is_verified_by_expert=a.is_verified_by_expert,
            is_indexed=a.is_indexed,
            created_at=a.created_at,
            updated_at=a.updated_at,
        )


class CreateArticleRequest(BaseModel):
    title: str
    content: str
    category: str | None = None


class EditSuggestionRequest(BaseModel):
    edited_response: str
