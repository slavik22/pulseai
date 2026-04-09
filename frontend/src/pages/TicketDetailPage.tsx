import { useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { formatDistanceToNow, format } from 'date-fns'
import { AiSuggestionPanel } from '../components/ai/AiSuggestionPanel'
import { useTicket, useAssignTicket, useResolveTicket } from '../hooks/useTickets'
import { useAiSuggestion } from '../hooks/useAiSuggestion'
import { useAuthStore } from '../store/authStore'
import { clsx } from 'clsx'
import type { Priority, TicketStatus } from '../types'

const STATUS_STYLES: Record<TicketStatus, string> = {
  OPEN: 'bg-amber-100 text-amber-800 border-amber-200',
  IN_PROGRESS: 'bg-blue-100 text-blue-800 border-blue-200',
  RESOLVED: 'bg-emerald-100 text-emerald-800 border-emerald-200',
  CLOSED: 'bg-gray-100 text-gray-600 border-gray-200',
}

const PRIORITY_STYLES: Record<Priority, string> = {
  LOW: 'bg-slate-100 text-slate-600',
  MEDIUM: 'bg-sky-100 text-sky-700',
  HIGH: 'bg-orange-100 text-orange-700',
  CRITICAL: 'bg-rose-100 text-rose-700',
}

export function TicketDetailPage() {
  const { ticketId } = useParams<{ ticketId: string }>()
  const navigate = useNavigate()
  const agent = useAuthStore((s) => s.agent)

  const [resolutionNote, setResolutionNote] = useState('')
  const [showResolveForm, setShowResolveForm] = useState(false)
  const [resolveError, setResolveError] = useState('')

  const { data: ticket, isLoading: ticketLoading } = useTicket(ticketId!)
  const { data: suggestion, isLoading: aiLoading } = useAiSuggestion(ticketId!)
  const assign = useAssignTicket()
  const resolve = useResolveTicket()

  if (ticketLoading) {
    return (
      <div className="flex items-center justify-center h-screen">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600" />
      </div>
    )
  }

  if (!ticket) {
    return (
      <div className="flex flex-col items-center justify-center h-screen gap-3">
        <p className="text-gray-500">Ticket not found.</p>
        <button onClick={() => navigate('/')} className="text-indigo-600 text-sm font-medium hover:underline">
          Back to Dashboard
        </button>
      </div>
    )
  }

  const canAssign = ticket.status === 'OPEN' && agent
  const canResolve = ticket.status === 'IN_PROGRESS' && ticket.assignedAgentId === agent?.id

  const handleResolve = async () => {
    if (!resolutionNote.trim()) { setResolveError('Please describe how the ticket was resolved.'); return }
    setResolveError('')
    resolve.mutate({ ticketId: ticket.id, payload: { resolutionNote } })
  }

  return (
    <div className="p-8 max-w-7xl">
      {/* Breadcrumb */}
      <div className="flex items-center gap-2 text-sm text-gray-500 mb-6">
        <button onClick={() => navigate('/')} className="hover:text-indigo-600 transition-colors font-medium">
          Dashboard
        </button>
        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
        </svg>
        <span className="text-gray-900 font-medium truncate max-w-xs">{ticket.title}</span>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-5 gap-6">

        {/* ── Left: Ticket Details (3/5) ── */}
        <div className="lg:col-span-3 space-y-5">

          {/* Title + status */}
          <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-6">
            <div className="flex items-start justify-between gap-4 mb-4">
              <h1 className="text-xl font-bold text-gray-900 leading-snug">{ticket.title}</h1>
              <span className={clsx(
                'shrink-0 text-xs font-semibold px-3 py-1.5 rounded-full border',
                STATUS_STYLES[ticket.status]
              )}>
                {ticket.status.replace('_', ' ')}
              </span>
            </div>

            {/* Meta row */}
            <div className="flex flex-wrap gap-2 mb-5">
              <span className={clsx('text-xs font-semibold px-2.5 py-1 rounded-lg', PRIORITY_STYLES[ticket.priority])}>
                {ticket.priority}
              </span>
              {ticket.category && (
                <span className="text-xs font-medium px-2.5 py-1 rounded-lg bg-indigo-50 text-indigo-600 capitalize">
                  {ticket.category}
                </span>
              )}
              <span className="text-xs text-gray-400 flex items-center gap-1">
                <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                {formatDistanceToNow(new Date(ticket.createdAt), { addSuffix: true })}
              </span>
              <span className="text-xs text-gray-400">
                #{ticket.id.slice(0, 8).toUpperCase()}
              </span>
            </div>

            {/* Content */}
            <div className="bg-slate-50 rounded-xl p-4">
              <p className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-2">Customer Message</p>
              <p className="text-gray-700 text-sm leading-relaxed whitespace-pre-wrap">{ticket.content}</p>
            </div>

            {/* Customer */}
            <div className="mt-4 flex items-center gap-2 text-xs text-gray-500">
              <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
              </svg>
              Customer: <span className="font-medium text-gray-700">{ticket.customerId}</span>
            </div>
          </div>

          {/* Timeline */}
          <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-6">
            <h2 className="text-sm font-semibold text-gray-500 uppercase tracking-wider mb-4">Timeline</h2>
            <div className="space-y-3">
              <TimelineItem
                icon="📋"
                label="Ticket created"
                time={ticket.createdAt}
                color="bg-slate-100"
              />
              {ticket.assignedAgentName && (
                <TimelineItem
                  icon="👤"
                  label={`Assigned to ${ticket.assignedAgentName}`}
                  time={ticket.updatedAt}
                  color="bg-blue-100"
                />
              )}
              {ticket.resolvedAt && (
                <TimelineItem
                  icon="✅"
                  label="Ticket resolved"
                  time={ticket.resolvedAt}
                  color="bg-emerald-100"
                />
              )}
            </div>
          </div>

          {/* Resolution note */}
          {ticket.resolutionNote && (
            <div className="bg-emerald-50 rounded-2xl border border-emerald-200 p-5">
              <p className="text-xs font-semibold text-emerald-600 uppercase tracking-wider mb-2">Resolution</p>
              <p className="text-gray-700 text-sm leading-relaxed">{ticket.resolutionNote}</p>
            </div>
          )}

          {/* Actions */}
          <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-5 space-y-3">
            <h2 className="text-sm font-semibold text-gray-500 uppercase tracking-wider">Actions</h2>

            {canAssign && (
              <button
                onClick={() => assign.mutate({ ticketId: ticket.id, payload: { agentId: agent!.id, agentName: agent!.name } })}
                disabled={assign.isPending}
                className="w-full rounded-xl bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50 text-white font-semibold py-3 text-sm transition-colors shadow-sm"
              >
                {assign.isPending ? 'Assigning…' : '👤 Assign to Me'}
              </button>
            )}

            {canResolve && !showResolveForm && (
              <button
                onClick={() => setShowResolveForm(true)}
                className="w-full rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-semibold py-3 text-sm transition-colors shadow-sm"
              >
                ✅ Mark as Resolved
              </button>
            )}

            {showResolveForm && (
              <div className="space-y-3">
                <textarea
                  className="w-full rounded-xl border border-gray-200 p-3 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-400 min-h-[100px] resize-none placeholder-gray-400"
                  placeholder="Describe what was done to resolve this ticket…"
                  value={resolutionNote}
                  onChange={(e) => { setResolutionNote(e.target.value); setResolveError('') }}
                />
                {resolveError && (
                  <p className="text-xs text-rose-600">{resolveError}</p>
                )}
                <div className="flex gap-2">
                  <button
                    onClick={handleResolve}
                    disabled={resolve.isPending}
                    className="flex-1 rounded-xl bg-emerald-600 hover:bg-emerald-700 disabled:opacity-50 text-white font-semibold py-2.5 text-sm transition-colors"
                  >
                    {resolve.isPending ? 'Resolving…' : 'Confirm Resolution'}
                  </button>
                  <button
                    onClick={() => { setShowResolveForm(false); setResolveError('') }}
                    className="px-4 rounded-xl border border-gray-200 text-sm font-medium text-gray-600 hover:bg-gray-50 transition-colors"
                  >
                    Cancel
                  </button>
                </div>
              </div>
            )}

            {!canAssign && !canResolve && ticket.status !== 'RESOLVED' && ticket.status !== 'CLOSED' && (
              <p className="text-sm text-gray-400 text-center py-2">No actions available for this ticket.</p>
            )}

            {(ticket.status === 'RESOLVED' || ticket.status === 'CLOSED') && (
              <div className="text-center py-2 text-sm text-gray-400">
                This ticket is {ticket.status.toLowerCase()}.
              </div>
            )}
          </div>
        </div>

        {/* ── Right: AI Suggestion (2/5) ── */}
        <div className="lg:col-span-2">
          {aiLoading ? (
            <div className="bg-violet-50 rounded-2xl border border-violet-200 p-6 flex items-center gap-3">
              <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-violet-600 shrink-0" />
              <div>
                <p className="text-sm font-medium text-violet-800">AI generating response…</p>
                <p className="text-xs text-violet-600 mt-0.5">Searching knowledge base with RAG</p>
              </div>
            </div>
          ) : suggestion ? (
            <AiSuggestionPanel suggestion={suggestion} ticketId={ticket.id} />
          ) : (
            <div className="bg-gray-50 rounded-2xl border border-gray-200 p-6 text-center">
              <div className="w-12 h-12 rounded-xl bg-gray-100 flex items-center justify-center mx-auto mb-3">
                <svg className="w-6 h-6 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5}
                    d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
                </svg>
              </div>
              <p className="text-sm font-medium text-gray-500">No AI suggestion available</p>
              <p className="text-xs text-gray-400 mt-1">Add knowledge articles to improve suggestions</p>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

function TimelineItem({ icon, label, time, color }: {
  icon: string; label: string; time: string; color: string
}) {
  return (
    <div className="flex items-center gap-3">
      <div className={`w-7 h-7 rounded-full ${color} flex items-center justify-center text-sm shrink-0`}>
        {icon}
      </div>
      <div className="flex-1">
        <p className="text-sm font-medium text-gray-700">{label}</p>
        <p className="text-xs text-gray-400">{format(new Date(time), 'MMM d, yyyy · HH:mm')}</p>
      </div>
    </div>
  )
}
