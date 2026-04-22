from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession

from src.ai.application.dto import (
    CreateArticleRequest,
    EditSuggestionRequest,
    KnowledgeArticleResponse,
    SuggestionResponse,
)
from src.ai.application.service import AiApplicationService, SuggestionNotFoundError
from src.ai.infrastructure.persistence import (
    SqlAlchemyAiSuggestionRepository,
    SqlAlchemyKnowledgeArticleRepository,
)
from src.database import get_db
from src.dependencies import get_rag_adapter

router = APIRouter(tags=["ai"])


def _get_service(
    db: AsyncSession = Depends(get_db),
    rag=Depends(get_rag_adapter),
) -> AiApplicationService:
    return AiApplicationService(
        rag=rag,
        suggestion_repo=SqlAlchemyAiSuggestionRepository(db),
        article_repo=SqlAlchemyKnowledgeArticleRepository(db),
    )


# ── Suggestions ────────────────────────────────────────────────────────────

@router.get("/tickets/{ticket_id}/suggestion", response_model=SuggestionResponse, tags=["suggestions"])
async def get_suggestion(
    ticket_id: str,
    service: AiApplicationService = Depends(_get_service),
) -> SuggestionResponse:
    try:
        return await service.get_suggestion_by_ticket_id(ticket_id)
    except SuggestionNotFoundError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(exc)) from exc


@router.patch("/suggestions/{suggestion_id}/accept", response_model=SuggestionResponse, tags=["suggestions"])
async def accept_suggestion(
    suggestion_id: UUID,
    service: AiApplicationService = Depends(_get_service),
) -> SuggestionResponse:
    try:
        return await service.accept_suggestion(suggestion_id)
    except SuggestionNotFoundError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(exc)) from exc


@router.patch("/suggestions/{suggestion_id}/reject", response_model=SuggestionResponse, tags=["suggestions"])
async def reject_suggestion(
    suggestion_id: UUID,
    service: AiApplicationService = Depends(_get_service),
) -> SuggestionResponse:
    try:
        return await service.reject_suggestion(suggestion_id)
    except SuggestionNotFoundError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(exc)) from exc


@router.patch("/suggestions/{suggestion_id}/edit", response_model=SuggestionResponse, tags=["suggestions"])
async def edit_suggestion(
    suggestion_id: UUID,
    request: EditSuggestionRequest,
    service: AiApplicationService = Depends(_get_service),
) -> SuggestionResponse:
    try:
        return await service.edit_suggestion(suggestion_id, request.edited_response)
    except SuggestionNotFoundError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(exc)) from exc


# ── Knowledge base ─────────────────────────────────────────────────────────

@router.get("/knowledge", response_model=list[KnowledgeArticleResponse], tags=["knowledge"])
async def list_articles(
    service: AiApplicationService = Depends(_get_service),
) -> list[KnowledgeArticleResponse]:
    return await service.get_all_articles()


@router.post("/knowledge", response_model=KnowledgeArticleResponse, status_code=status.HTTP_201_CREATED, tags=["knowledge"])
async def create_article(
    request: CreateArticleRequest,
    service: AiApplicationService = Depends(_get_service),
) -> KnowledgeArticleResponse:
    return await service.create_and_index_article(request)


@router.delete("/knowledge/{article_id}", status_code=status.HTTP_204_NO_CONTENT, tags=["knowledge"])
async def delete_article(
    article_id: UUID,
    service: AiApplicationService = Depends(_get_service),
) -> None:
    await service.delete_article(article_id)
