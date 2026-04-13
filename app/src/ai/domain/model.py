"""
AI domain models — AiSuggestion, KnowledgeArticle, ConfidenceScore.
"""
from __future__ import annotations

import uuid
from dataclasses import dataclass, field
from datetime import UTC, datetime
from enum import StrEnum


class SuggestionStatus(StrEnum):
    PENDING = "PENDING"
    ACCEPTED = "ACCEPTED"
    REJECTED = "REJECTED"
    EDITED = "EDITED"


@dataclass
class ConfidenceScore:
    value: float  # 0.0 – 1.0

    RELIABLE_THRESHOLD: float = field(default=0.65, init=False, repr=False)

    def __post_init__(self) -> None:
        if not (0.0 <= self.value <= 1.0):
            raise ValueError(f"Confidence must be in [0, 1], got {self.value}")

    @property
    def is_reliable(self) -> bool:
        return self.value >= 0.65

    def __str__(self) -> str:
        return f"{self.value:.0%}"


@dataclass
class AiSuggestion:
    id: uuid.UUID
    ticket_id: str
    draft_response: str
    confidence: ConfidenceScore
    status: SuggestionStatus
    source_article_ids: list[str]
    created_at: datetime
    updated_at: datetime
    edited_response: str | None = None

    @classmethod
    def create(
        cls,
        ticket_id: str,
        draft_response: str,
        confidence: ConfidenceScore,
        source_article_ids: list[str],
    ) -> "AiSuggestion":
        now = datetime.now(UTC)
        return cls(
            id=uuid.uuid4(),
            ticket_id=ticket_id,
            draft_response=draft_response,
            confidence=confidence,
            status=SuggestionStatus.PENDING,
            source_article_ids=source_article_ids,
            created_at=now,
            updated_at=now,
        )

    def accept(self) -> None:
        self.status = SuggestionStatus.ACCEPTED
        self.updated_at = datetime.now(UTC)

    def reject(self) -> None:
        self.status = SuggestionStatus.REJECTED
        self.updated_at = datetime.now(UTC)

    def edit_and_accept(self, edited_response: str) -> None:
        self.edited_response = edited_response
        self.status = SuggestionStatus.EDITED
        self.updated_at = datetime.now(UTC)


@dataclass
class KnowledgeArticle:
    id: uuid.UUID
    title: str
    content: str
    category: str | None
    is_verified_by_expert: bool
    is_indexed: bool
    created_at: datetime
    updated_at: datetime

    @classmethod
    def create(cls, title: str, content: str, category: str | None = None) -> "KnowledgeArticle":
        now = datetime.now(UTC)
        return cls(
            id=uuid.uuid4(),
            title=title,
            content=content,
            category=category,
            is_verified_by_expert=False,
            is_indexed=False,
            created_at=now,
            updated_at=now,
        )

    def mark_indexed(self) -> None:
        self.is_indexed = True
        self.updated_at = datetime.now(UTC)
