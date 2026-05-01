"""
Prometheus metrics registry for PulseAI.

Counters and timers are created once at import time and reused across
the entire process — matching Micrometer's singleton MeterRegistry pattern.
"""
from prometheus_client import Counter, Histogram

# ── AI suggestions ────────────────────────────────────────────────────────────

ai_suggestions_total = Counter(
    "ai_suggestions_total",
    "Total AI suggestion generation attempts",
)
ai_suggestions_low_confidence = Counter(
    "ai_suggestions_low_confidence_total",
    "Suggestions generated with below-threshold confidence",
)
ai_suggestions_accepted = Counter(
    "ai_suggestions_accepted_total",
    "Suggestions accepted by agents without edits",
)
ai_suggestions_rejected = Counter(
    "ai_suggestions_rejected_total",
    "Suggestions rejected by agents",
)
ai_suggestions_edited = Counter(
    "ai_suggestions_edited_total",
    "Suggestions edited before acceptance by agents",
)

# ── RAG pipeline ─────────────────────────────────────────────────────────────

rag_latency_seconds = Histogram(
    "ai_rag_latency_seconds",
    "End-to-end latency of a single RAG retrieval + generation cycle",
    buckets=[0.5, 1.0, 2.0, 5.0, 10.0, 20.0, 30.0],
)

# ── Tickets ───────────────────────────────────────────────────────────────────

tickets_created_total = Counter(
    "tickets_created_total",
    "Total tickets created",
)
tickets_resolved_total = Counter(
    "tickets_resolved_total",
    "Total tickets resolved",
)
