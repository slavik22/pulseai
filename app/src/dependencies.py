"""
FastAPI dependency providers for singleton services (RAG adapter, etc.).

The RAG adapter is expensive to construct (it opens a PGVector connection
pool) so it is built once at startup and stored on the app's state object,
then retrieved here as a FastAPI dependency.
"""
from fastapi import Request

from src.ai.domain.rag_port import RagPort


def get_rag_adapter(request: Request) -> RagPort:
    return request.app.state.rag_adapter
