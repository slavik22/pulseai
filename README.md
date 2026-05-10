# PulseAI — AI-Powered Support Desk

A full-stack support desk platform with AI-assisted ticket resolution, real-time notifications, and a React frontend. Built with Spring Boot (hexagonal architecture / DDD) on the backend and React + TypeScript + Tailwind on the frontend.

---

## Table of Contents

- [Features](#features)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [API Reference](#api-reference)
- [CI/CD](#cicd)
- [GitFlow](#gitflow)

---

## Features

- **Ticket management** — create, assign, and resolve support tickets with full lifecycle tracking
- **AI suggestions** — RAG-powered suggestions generated automatically when a ticket is created (LangChain4j + OpenAI GPT-4o)
- **Knowledge base** — searchable articles used as context for AI responses
- **Real-time notifications** — WebSocket (STOMP) push notifications on ticket updates
- **Observability** — Prometheus metrics + Grafana dashboards out of the box
- **Virtual threads** — Project Loom enabled for high-concurrency I/O

---

## Architecture

The backend follows **Hexagonal Architecture** (Ports & Adapters) combined with **Domain-Driven Design**.

```
┌─────────────────────────────────────────────────────────────┐
│                        Frontend (React)                      │
│          Vite · TypeScript · Tailwind · Zustand             │
└──────────────────────────┬──────────────────────────────────┘
                           │ HTTP / WebSocket
┌──────────────────────────▼──────────────────────────────────┐
│                   Spring Boot Application                    │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐    │
│  │                   REST Controllers                   │    │
│  │         TicketController · AiController             │    │
│  └────────────────────────┬────────────────────────────┘    │
│                           │                                  │
│  ┌────────────────────────▼────────────────────────────┐    │
│  │              Application Layer (Use Cases)           │    │
│  │   TicketApplicationService · AiApplicationService   │    │
│  └────────────────────────┬────────────────────────────┘    │
│                           │                                  │
│  ┌────────────────────────▼────────────────────────────┐    │
│  │                    Domain Layer                      │    │
│  │   Ticket · AiSuggestion · KnowledgeArticle          │    │
│  │   Domain Events · Value Objects · Repository Ports  │    │
│  └────────────────────────┬────────────────────────────┘    │
│                           │                                  │
│  ┌────────────────────────▼────────────────────────────┐    │
│  │               Infrastructure Adapters               │    │
│  │   JPA Repositories · LangChain4j RAG · WebSocket   │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                              │
└──────────────────────────┬──────────────────────────────────┘
                           │
          ┌────────────────┼────────────────┐
          ▼                ▼                ▼
      PostgreSQL        OpenAI          Prometheus
```

### Domain modules

| Module | Responsibility |
|---|---|
| `ticket` | Core business logic — ticket lifecycle, state machine, domain events |
| `ai` | AI suggestion generation via RAG, knowledge article management |
| `notification` | Real-time WebSocket push notifications |
| `shared` | Cross-cutting concerns — exception handling |

### Key design decisions

- **Hexagonal architecture** — domain logic has zero dependencies on Spring or JPA. Fully testable in isolation.
- **Domain events** — `Ticket` raises `TicketCreatedEvent`, `TicketAssignedEvent`, `TicketResolvedEvent`. Published via `ApplicationEventPublisher` after DB commit (`@TransactionalEventListener`).
- **Async listeners** — AI suggestion generation and WebSocket notifications run in a separate thread (`@Async`) so HTTP response returns immediately.
- **Virtual threads** — `spring.threads.virtual.enabled=true` (Project Loom) gives high concurrency without tuning thread pools.
- **Flyway** — all schema changes are versioned migrations, never `ddl-auto: create`.

---

## Tech Stack

### Backend
| Technology | Version | Purpose |
|---|---|---|
| Java | 21 | Language (virtual threads via Project Loom) |
| Spring Boot | 3.2 | Application framework |
| Spring Data JPA | 3.2 | Persistence |
| PostgreSQL | 15 | Primary database |
| Flyway | 9 | Database migrations |
| LangChain4j | 0.35 | AI / RAG integration |
| OpenAI GPT-4o | — | LLM for suggestions |
| WebSocket (STOMP) | — | Real-time notifications |
| Micrometer + Prometheus | — | Metrics |

### Frontend
| Technology | Version | Purpose |
|---|---|---|
| React | 18 | UI framework |
| TypeScript | 5.4 | Type safety |
| Vite | 5 | Build tool |
| Tailwind CSS | 3.4 | Styling |
| Zustand | 4.5 | State management |
| TanStack Query | 5 | Server state / caching |
| Axios | 1.7 | HTTP client |
| STOMP.js | 7 | WebSocket client |
| Recharts | 2 | Charts and metrics |

### Infrastructure
| Technology | Purpose |
|---|---|
| Docker + Docker Compose | Local development stack |
| GitHub Actions | CI/CD pipelines |
| Prometheus | Metrics collection |
| Grafana | Metrics dashboards |
| GHCR | Container registry |

---

## Project Structure

```
supportDesk/
├── app/                              # Spring Boot backend
│   ├── src/main/java/com/pulseai/
│   │   ├── ticket/
│   │   │   ├── domain/model/         # Ticket aggregate, value objects, events
│   │   │   ├── application/          # Use cases, DTOs, application service
│   │   │   └── infrastructure/       # JPA adapters, REST controller
│   │   ├── ai/
│   │   │   ├── domain/model/         # AiSuggestion, KnowledgeArticle, ConfidenceScore
│   │   │   ├── application/          # AiApplicationService
│   │   │   └── infrastructure/       # LangChain4j RAG, JPA, REST
│   │   ├── notification/
│   │   │   ├── application/service/  # NotificationService
│   │   │   └── infrastructure/       # WebSocket config
│   │   ├── event/                    # TicketEventListener
│   │   └── shared/                   # GlobalExceptionHandler
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/             # Flyway SQL migrations
│   └── src/test/                     # Unit + integration tests
├── frontend/                         # React + TypeScript SPA
│   ├── src/
│   │   ├── api/                      # Axios API clients
│   │   ├── components/               # Reusable UI components
│   │   ├── hooks/                    # Custom React hooks
│   │   ├── pages/                    # Route-level page components
│   │   ├── store/                    # Zustand stores
│   │   └── types/                    # Shared TypeScript types
│   ├── Dockerfile
│   └── nginx.conf
├── infra/
│   ├── postgres/init.sql
│   ├── prometheus/prometheus.yml
│   └── grafana/
├── .github/workflows/                # CI/CD pipelines
│   ├── backend-ci.yml
│   ├── frontend-ci.yml
│   ├── docker-build.yml
│   └── release.yml
└── docker-compose.yml
```

---

## Getting Started

### Prerequisites

- Docker & Docker Compose
- Java 21 (for local backend development)
- Node.js 20 (for local frontend development)
- An OpenAI API key

### Run with Docker Compose

```bash
# Clone the repo
git clone https://github.com/slavik22/pulseai.git
cd pulseai

# Set your OpenAI key
cp .env.example .env
# Edit .env and set OPENAI_API_KEY=sk-...

# Start everything
docker compose up -d

# Services:
# Frontend:   http://localhost:3000
# Backend:    http://localhost:8080
# Grafana:    http://localhost:3001  (admin/admin)
# Prometheus: http://localhost:9090
```

### Run backend locally

```bash
cd app
mvn spring-boot:run
# Requires PostgreSQL running on localhost:5432
```

### Run frontend locally

```bash
cd frontend
npm install
npm run dev
# http://localhost:5173
```

### Run tests

```bash
# Backend
cd app && mvn test

# Frontend
cd frontend && npm test
```

---

## API Reference

### Tickets

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/tickets` | Create a new ticket |
| `GET` | `/api/tickets/{id}` | Get ticket by ID |
| `GET` | `/api/tickets/customer/{customerId}` | Get tickets by customer |
| `GET` | `/api/tickets/agent/{agentId}` | Get tickets assigned to agent |
| `PUT` | `/api/tickets/{id}/assign` | Assign ticket to agent |
| `PUT` | `/api/tickets/{id}/resolve` | Resolve a ticket |

### AI

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/ai/suggestions/{ticketId}` | Get AI suggestions for a ticket |
| `GET` | `/api/ai/knowledge` | List knowledge articles |
| `POST` | `/api/ai/knowledge` | Add a knowledge article |

### WebSocket

Connect to `ws://localhost:8080/ws` using STOMP.

| Topic | Description |
|---|---|
| `/topic/tickets` | Ticket lifecycle events |
| `/topic/notifications/{userId}` | User-specific notifications |

### Health & Metrics

```
GET /actuator/health
GET /actuator/metrics
GET /actuator/prometheus
```

---

## CI/CD

Four GitHub Actions workflows run automatically:

| Workflow | Trigger | What it does |
|---|---|---|
| `backend-ci.yml` | Push to `main`/`develop` (app changes) | Spins up Postgres, runs `mvn verify` |
| `frontend-ci.yml` | Push to `main`/`develop` (frontend changes) | Type-check, Vitest, `npm run build` |
| `docker-build.yml` | Push to `main` or release | Builds and pushes Docker images to GHCR |
| `release.yml` | Tag `v*.*.*` pushed | Builds artifacts, creates GitHub Release |

---

## GitFlow

This project follows [GitFlow](https://nvie.com/posts/a-successful-git-branching-model/):

```
main ──────────────────── v1.0.0 ──── v1.0.1
        \                /      \    /
develop  \──────────────/        \──/
           \  \  \  \
feature/*   \  \  \  \── merged via PR
release/*        \────── prep, then merged to main + develop
hotfix/*              \─ emergency fix from main
```

| Branch | Purpose |
|---|---|
| `main` | Production only. Every commit is deployable. |
| `develop` | Integration branch. All features merge here first. |
| `feature/*` | One branch per feature, branched from `develop`. |
| `release/*` | Release preparation (version bump, final fixes). |
| `hotfix/*` | Emergency production fixes branched from `main`. |

**Releases:** `v1.0.0` (initial release), `v1.0.1` (hotfix — ticket assignment guard).
