"""
LangChain RAG adapter — the Python-native equivalent of LangChain4jRagAdapter.

Pipeline:
  1. Embed the ticket content with text-embedding-3-small
  2. Retrieve the top-K most similar KB chunks from pgvector
  3. Filter matches below MIN_SIMILARITY threshold
  4. Build a context block from the retrieved chunks
  5. Call GPT-4o with a strict system prompt to draft a response
  6. Wrap result in an AiSuggestion with a ConfidenceScore

All I/O is async — the embedding call and LLM call use the async OpenAI client.
"""
from __future__ import annotations

import logging
import time
from typing import Any

from langchain.text_splitter import RecursiveCharacterTextSplitter
from langchain_core.documents import Document
from langchain_openai import ChatOpenAI, OpenAIEmbeddings
from langchain_postgres import PGVector

from src.ai.domain.model import AiSuggestion, ConfidenceScore, KnowledgeArticle
from src.ai.domain.rag_port import RagPort
from src.config import settings
from src.shared import metrics

log = logging.getLogger(__name__)

SYSTEM_PROMPT = """\
You are an expert customer support agent for a SaaS platform.
Draft a helpful, empathetic response to the customer ticket below.

Rules:
1. Use ONLY the information in the CONTEXT section.
2. If context is insufficient, say you will investigate and follow up.
3. Do NOT invent facts, prices, or timelines.
4. Be concise (2–4 paragraphs) and professional.
5. End with: "Please let us know if you need anything else."
{warning}

CONTEXT:
{context}
"""


class LangChainRagAdapter(RagPort):
    """Concrete RAG implementation using LangChain + OpenAI + PGVector."""

    def __init__(
        self,
        connection_string: str,
        embeddings: OpenAIEmbeddings,
        llm: ChatOpenAI,
    ) -> None:
        self._embeddings = embeddings
        self._llm = llm
        self._splitter = RecursiveCharacterTextSplitter(
            chunk_size=settings.rag_chunk_size,
            chunk_overlap=settings.rag_chunk_overlap,
        )
        # PGVector store — collection maps to a pg table under the hood
        self._store = PGVector(
            embeddings=embeddings,
            collection_name="knowledge_base",
            connection=connection_string,
            use_jsonb=True,
        )

    # ── Public API ────────────────────────────────────────────────────────────

    async def generate_suggestion(
        self,
        ticket_id: str,
        ticket_content: str,
        category: str | None,
    ) -> AiSuggestion:
        metrics.ai_suggestions_total.inc()
        start = time.perf_counter()
        try:
            suggestion = await self._do_generate(ticket_id, ticket_content, category)
        finally:
            metrics.rag_latency_seconds.observe(time.perf_counter() - start)
        return suggestion

    async def index_article(self, article: KnowledgeArticle) -> None:
        doc = Document(
            page_content=f"{article.title}\n\n{article.content}",
            metadata={
                "article_id": str(article.id),
                "article_title": article.title,
                "category": article.category or "general",
                "verified": article.is_verified_by_expert,
            },
        )
        chunks = self._splitter.split_documents([doc])
        await self._store.aadd_documents(chunks)
        log.info("Indexed %d chunks for article %s", len(chunks), article.id)

    async def remove_article_embeddings(self, article_id: str) -> None:
        await self._store.adelete(filter={"article_id": article_id})
        log.info("Removed embeddings for article %s", article_id)

    # ── Internal ──────────────────────────────────────────────────────────────

    async def _do_generate(
        self,
        ticket_id: str,
        ticket_content: str,
        category: str | None,
    ) -> AiSuggestion:
        # Retrieval
        matches = await self._store.asimilarity_search_with_relevance_scores(
            ticket_content,
            k=settings.rag_top_k,
        )
        relevant = [(doc, score) for doc, score in matches if score >= settings.rag_min_similarity]

        avg_score = (
            sum(score for _, score in relevant) / len(relevant) if relevant else 0.0
        )
        confidence = ConfidenceScore(value=round(avg_score, 4))

        if not confidence.is_reliable:
            metrics.ai_suggestions_low_confidence.inc()
            log.warning("Low confidence (%s) for ticket %s", confidence, ticket_id)

        context = self._build_context(relevant)
        source_ids = list(
            {doc.metadata.get("article_id", "") for doc, _ in relevant if doc.metadata.get("article_id")}
        )

        draft = await self._generate_draft(ticket_content, context, confidence)
        return AiSuggestion.create(
            ticket_id=ticket_id,
            draft_response=draft,
            confidence=confidence,
            source_article_ids=source_ids,
        )

    async def _generate_draft(
        self,
        ticket_content: str,
        context: str,
        confidence: ConfidenceScore,
    ) -> str:
        warning = "\nWARNING: Limited context available. Be conservative and avoid guessing." if not confidence.is_reliable else ""
        system = SYSTEM_PROMPT.format(
            warning=warning,
            context=context or "No relevant knowledge base articles found.",
        )
        from langchain_core.messages import HumanMessage, SystemMessage
        response = await self._llm.ainvoke(
            [
                SystemMessage(content=system),
                HumanMessage(content=f"CUSTOMER TICKET:\n{ticket_content}"),
            ]
        )
        return str(response.content)

    @staticmethod
    def _build_context(matches: list[tuple[Document, float]]) -> str:
        if not matches:
            return ""
        parts = []
        for doc, score in matches:
            title = doc.metadata.get("article_title", "KB Article")
            parts.append(f"[{title}] ({score:.0%})\n{doc.page_content}")
        return "\n\n---\n\n".join(parts)


def build_rag_adapter(sync_connection_string: str) -> LangChainRagAdapter:
    """Factory — called once at app startup."""
    embeddings = OpenAIEmbeddings(
        model=settings.openai_embedding_model,
        api_key=settings.openai_api_key,
    )
    llm = ChatOpenAI(
        model=settings.openai_chat_model,
        api_key=settings.openai_api_key,
        temperature=0.3,
    )
    return LangChainRagAdapter(
        connection_string=sync_connection_string,
        embeddings=embeddings,
        llm=llm,
    )
