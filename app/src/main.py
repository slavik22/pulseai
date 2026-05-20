"""
PulseAI — FastAPI application entry point.

Startup sequence:
  1. Build the async SQLAlchemy engine
  2. Build the LangChain RAG adapter (connects to pgvector)
  3. Create a long-lived AiApplicationService + NotificationService
  4. Register event handlers so ticket events flow to AI + WebSockets
  5. Mount Prometheus instrumentation
  6. Register all API routers
"""
from __future__ import annotations

import logging
from contextlib import asynccontextmanager
from typing import AsyncGenerator

import structlog
from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from prometheus_fastapi_instrumentator import Instrumentator

from src.ai.infrastructure.rag import build_rag_adapter
from src.ai.infrastructure.router import router as ai_router
from src.config import settings
from src.database import AsyncSessionLocal, engine
from src.event.listener import register_event_handlers
from src.notification.router import router as ws_router
from src.notification.service import NotificationService
from src.ticket.infrastructure.router import router as ticket_router

# ── Logging ───────────────────────────────────────────────────────────────────

structlog.configure(
    wrapper_class=structlog.make_filtering_bound_logger(
        logging.getLevelName(settings.log_level)
    ),
)
logging.basicConfig(
    level=settings.log_level,
    format="%(asctime)s | %(levelname)-8s | %(name)s | %(message)s",
)


# ── Lifespan ──────────────────────────────────────────────────────────────────

@asynccontextmanager
async def lifespan(app: FastAPI) -> AsyncGenerator[None, None]:
    # Build singletons
    rag_adapter = build_rag_adapter(settings.database_url_sync)
    app.state.rag_adapter = rag_adapter
    app.state.session_factory = AsyncSessionLocal

    # Wire event handlers — each handler opens its own short-lived session
    # so it is decoupled from request sessions that may already be committed.
    notification_service = NotificationService()
    register_event_handlers(
        session_factory=AsyncSessionLocal,
        rag_adapter=rag_adapter,
        notification_service=notification_service,
    )

    yield

    # Teardown
    await engine.dispose()


# ── Application ───────────────────────────────────────────────────────────────

def create_app() -> FastAPI:
    app = FastAPI(
        title="PulseAI",
        description=(
            "AI-powered customer support desk. "
            "Tickets are auto-triaged with RAG-generated response suggestions "
            "backed by a pgvector knowledge base."
        ),
        version="1.0.0",
        lifespan=lifespan,
        docs_url="/docs",
        redoc_url="/redoc",
    )

    # CORS — allow the Vite dev server and production frontend
    app.add_middleware(
        CORSMiddleware,
        allow_origins=["http://localhost:5173", "http://localhost:3000"],
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )

    # Prometheus metrics — exposed at /metrics
    Instrumentator(
        should_group_status_codes=True,
        should_ignore_untemplated=True,
        excluded_handlers=["/metrics", "/health"],
    ).instrument(app).expose(app, endpoint="/metrics")

    # Routers
    app.include_router(ticket_router, prefix="/api/v1")
    app.include_router(ai_router, prefix="/api/v1")
    app.include_router(ws_router)

    # Health check
    @app.get("/health", tags=["ops"])
    async def health() -> dict[str, str]:
        return {"status": "UP", "service": "pulseai"}

    # Global exception handlers
    @app.exception_handler(ValueError)
    async def value_error_handler(request: Request, exc: ValueError) -> JSONResponse:
        return JSONResponse(status_code=422, content={"detail": str(exc)})

    return app


app = create_app()
