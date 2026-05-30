# PulseAI — AI-Powered Customer Support Desk

A production-grade pet project demonstrating core **AI engineering** skills:
RAG pipeline, async Python, vector search, real-time notifications, and full observability.

---

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    React + TypeScript                    │
│              (Vite · Tailwind · WebSockets)              │
└──────────────────────┬──────────────────────────────────┘
                       │ HTTP / WebSocket
┌──────────────────────▼──────────────────────────────────┐
│                  FastAPI (Python 3.12)                   │
│                                                          │
│  ┌──────────┐  ┌──────────────┐  ┌───────────────────┐  │
│  │  Ticket  │  │  AI / RAG    │  │   Notification    │  │
│  │  module  │  │   module     │  │   (WebSocket)     │  │
│  └────┬─────┘  └──────┬───────┘  └────────▲──────────┘  │
│       │ domain events  │ LangChain          │             │
│       └────────────────┴──────── EventBus ─┘             │
└────────┬─────────────────────────┬───────────────────────┘
         │                         │
┌────────▼────────┐    ┌───────────▼──────────┐
│   PostgreSQL 16  │    │   OpenAI API          │
│   + pgvector     │    │   GPT-4o + embeddings │
└─────────────────┘    └──────────────────────┘
```

### Modules

| Module | Responsibility |
|---|---|
| `ticket` | Ticket aggregate lifecycle — OPEN → IN_PROGRESS → RESOLVED |
| `ai` | RAG pipeline: embed → retrieve → generate; suggestion feedback loop |
| `notification` | WebSocket broadcast for real-time UI updates |
| `event` | In-process async event bus (post-commit, non-blocking) |
| `shared` | Prometheus metrics, event types |

### AI Engineering highlights

- **RAG pipeline** (LangChain + pgvector): KB articles are chunked, embedded with `text-embedding-3-small`, stored in pgvector. On each new ticket, the top-K most similar chunks are retrieved and injected into a GPT-4o prompt.
- **Confidence scoring**: average cosine similarity of retrieved chunks drives a `ConfidenceScore` value object. Low-confidence suggestions get a conservative system prompt warning.
- **Async-first**: everything is `async/await` — DB via `asyncpg`, OpenAI via async client, background tasks via `asyncio.create_task`.
- **Non-blocking AI**: ticket HTTP response returns in `<50ms`; AI generation runs in a background task after the DB commit (mirrors Spring's `@Async` + `@TransactionalEventListener(AFTER_COMMIT)`).
- **Suggestion feedback loop**: agents accept/reject/edit suggestions — tracked with Prometheus counters to measure model performance over time.
- **Observability**: Prometheus metrics exposed at `/metrics`, scraped and visualised in Grafana.

---

## Quick start

### Prerequisites

- Docker + Docker Compose
- An OpenAI API key

### Run with Docker Compose

```bash
cp app/.env.example .env
# Set OPENAI_API_KEY in .env

docker compose up --build
```

| Service | URL |
|---|---|
| API + Swagger | http://localhost:8080/docs |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3001 (admin / admin) |

### Local development

```bash
cd app
python -m venv .venv && source .venv/bin/activate   # Windows: .venv\Scripts\activate
pip install -e ".[dev]"

cp .env.example .env   # fill in OPENAI_API_KEY

# Start Postgres
docker compose up postgres -d

# Run migrations
alembic upgrade head

# Start the API
uvicorn src.main:app --reload --port 8080
```

### Run tests

```bash
cd app
pytest tests/ -v
```

---

## API overview

### Tickets

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/v1/tickets` | Create a ticket |
| `GET` | `/api/v1/tickets` | List tickets (filter by `customerId` / `agentId`) |
| `GET` | `/api/v1/tickets/{id}` | Get ticket |
| `PATCH` | `/api/v1/tickets/{id}/assign` | Assign to agent |
| `PATCH` | `/api/v1/tickets/{id}/resolve` | Resolve with note |

### AI / RAG

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/v1/tickets/{id}/suggestion` | Get AI draft response |
| `PATCH` | `/api/v1/suggestions/{id}/accept` | Accept suggestion |
| `PATCH` | `/api/v1/suggestions/{id}/reject` | Reject suggestion |
| `PATCH` | `/api/v1/suggestions/{id}/edit` | Edit and accept |

### Knowledge base

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/v1/knowledge` | List articles |
| `POST` | `/api/v1/knowledge` | Create + index article |
| `DELETE` | `/api/v1/knowledge/{id}` | Delete + remove embeddings |

### Real-time

```
WebSocket: ws://localhost:8080/ws/{clientId}
```

Events pushed: `TICKET_CREATED`, `TICKET_ASSIGNED`, `TICKET_RESOLVED`

---

## Tech stack

| Layer | Technology |
|---|---|
| Web framework | FastAPI 0.115 |
| LLM / Embeddings | OpenAI GPT-4o + text-embedding-3-small |
| RAG | LangChain + langchain-postgres (PGVector) |
| Database | PostgreSQL 16 + pgvector |
| ORM / migrations | SQLAlchemy 2.0 async + Alembic |
| Validation | Pydantic v2 |
| Metrics | Prometheus + Grafana |
| Containerization | Docker + Docker Compose |
| Testing | pytest-asyncio |
| Linting | Ruff + mypy |
