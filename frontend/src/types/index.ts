// ── Ticket types ──────────────────────────────────────────────────────────────

export type TicketStatus = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED'
export type Priority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'

export interface Ticket {
  id: string
  title: string
  content: string
  status: TicketStatus
  priority: Priority
  customerId: string
  category: string | null
  assignedAgentId: string | null
  assignedAgentName: string | null
  resolutionNote: string | null
  createdAt: string
  updatedAt: string
  resolvedAt: string | null
}

export interface CreateTicketPayload {
  title: string
  content: string
  priority: Priority
  customerId: string
  category?: string
}

export interface AssignTicketPayload {
  agentId: string
  agentName: string
}

export interface ResolveTicketPayload {
  resolutionNote: string
}

// ── AI Suggestion types ───────────────────────────────────────────────────────

export type FeedbackStatus = 'PENDING' | 'ACCEPTED' | 'EDITED' | 'REJECTED'
export type ConfidenceLabel = 'HIGH' | 'MEDIUM' | 'LOW'

export interface AiSuggestion {
  id: string
  ticketId: string
  suggestedResponse: string
  confidenceScore: number
  confidenceLabel: ConfidenceLabel
  isReliable: boolean
  sourceArticleIds: string[]
  feedbackStatus: FeedbackStatus
  createdAt: string
}

// ── Knowledge Article types ───────────────────────────────────────────────────

export interface KnowledgeArticle {
  id: string
  title: string
  content: string
  category: string | null
  verifiedByExpert: boolean
  indexed: boolean
  createdAt: string
  updatedAt: string
  indexedAt: string | null
}

export interface CreateArticlePayload {
  title: string
  content: string
  category: string
}

// ── API error (RFC 7807 ProblemDetail) ────────────────────────────────────────

export interface ProblemDetail {
  type?: string
  title?: string
  status: number
  detail: string
  instance?: string
  errors?: Record<string, string>
}
