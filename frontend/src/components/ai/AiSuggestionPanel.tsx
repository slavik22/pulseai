import { useState } from 'react'
import { clsx } from 'clsx'
import type { AiSuggestion, ConfidenceLabel } from '../../types'
import { useAcceptSuggestion, useEditSuggestion, useRejectSuggestion } from '../../hooks/useAiSuggestion'

/**
 * The most interview-impressive component.
 *
 * Shows the RAG-generated draft with:
 * - Confidence score bar (visual quality indicator)
 * - Source article citations
 * - Accept / Reject / Edit workflow
 * - Edit mode with diff-like "before/after" view
 *
 * Every action (accept/reject/edit) sends feedback to ai-gateway-service
 * which increments Prometheus counters → Grafana tracks ai_acceptance_rate.
 * The UI IS the data collection mechanism for model quality measurement.
 */

const CONFIDENCE_COLORS: Record<ConfidenceLabel, string> = {
  HIGH: 'bg-green-500',
  MEDIUM: 'bg-yellow-500',
  LOW: 'bg-red-500',
}

const CONFIDENCE_TEXT: Record<ConfidenceLabel, string> = {
  HIGH: 'text-green-700',
  MEDIUM: 'text-yellow-700',
  LOW: 'text-red-700',
}

interface Props {
  suggestion: AiSuggestion
  ticketId: string
}

export function AiSuggestionPanel({ suggestion}: Props) {
  const [editMode, setEditMode] = useState(false)
  const [editedText, setEditedText] = useState(suggestion.suggestedResponse)

  const accept = useAcceptSuggestion()
  const reject = useRejectSuggestion()
  const edit = useEditSuggestion()

  const isPending = suggestion.feedbackStatus === 'PENDING'
  const isBusy = accept.isPending || reject.isPending || edit.isPending

  return (
    <div className="rounded-xl border border-purple-200 bg-purple-50 p-5 space-y-4">

      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <span className="text-lg">🤖</span>
          <span className="font-semibold text-purple-900">AI Suggested Response</span>
        </div>

        {/* Confidence score badge */}
        <div className="flex items-center gap-2">
          <span className={clsx('text-xs font-bold uppercase', CONFIDENCE_TEXT[suggestion.confidenceLabel])}>
            {suggestion.confidenceLabel} confidence
          </span>
          {/* Score bar */}
          <div className="w-24 h-2 bg-gray-200 rounded-full overflow-hidden">
            <div
              className={clsx('h-full rounded-full transition-all', CONFIDENCE_COLORS[suggestion.confidenceLabel])}
              style={{ width: `${Math.round(suggestion.confidenceScore * 100)}%` }}
            />
          </div>
          <span className="text-xs text-gray-500">{Math.round(suggestion.confidenceScore * 100)}%</span>
        </div>
      </div>

      {/* Low confidence warning */}
      {!suggestion.isReliable && (
        <div className="rounded-md bg-yellow-50 border border-yellow-200 px-3 py-2 text-sm text-yellow-800">
          ⚠️ Low confidence — the knowledge base may not cover this topic well. Review carefully before sending.
        </div>
      )}

      {/* Response text — edit mode or read mode */}
      {editMode ? (
        <textarea
          className="w-full rounded-lg border border-purple-300 p-3 text-sm text-gray-800 focus:outline-none focus:ring-2 focus:ring-purple-400 min-h-[160px] font-mono"
          value={editedText}
          onChange={(e) => setEditedText(e.target.value)}
        />
      ) : (
        <div className="rounded-lg bg-white border border-purple-100 p-4 text-sm text-gray-700 whitespace-pre-wrap leading-relaxed">
          {suggestion.suggestedResponse}
        </div>
      )}

      {/* Source articles */}
      {suggestion.sourceArticleIds.length > 0 && (
        <div className="text-xs text-gray-500">
          <span className="font-medium">Sources: </span>
          {suggestion.sourceArticleIds.map((id) => (
            <span key={id} className="inline-block bg-gray-100 rounded px-1.5 py-0.5 mr-1 font-mono">
              {id.slice(0, 8)}…
            </span>
          ))}
        </div>
      )}

      {/* Feedback status (post-action) */}
      {!isPending && (
        <div className={clsx(
          'text-sm font-medium rounded-md px-3 py-2 text-center',
          suggestion.feedbackStatus === 'ACCEPTED' && 'bg-green-100 text-green-800',
          suggestion.feedbackStatus === 'EDITED' && 'bg-blue-100 text-blue-800',
          suggestion.feedbackStatus === 'REJECTED' && 'bg-red-100 text-red-800',
        )}>
          {suggestion.feedbackStatus === 'ACCEPTED' && '✓ Accepted — thank you for the feedback!'}
          {suggestion.feedbackStatus === 'EDITED' && '✏️ Edited version saved'}
          {suggestion.feedbackStatus === 'REJECTED' && '✗ Rejected — feedback recorded'}
        </div>
      )}

      {/* Action buttons — only shown while PENDING */}
      {isPending && (
        <div className="flex gap-2">
          {!editMode ? (
            <>
              <button
                onClick={() => accept.mutate(suggestion.id)}
                disabled={isBusy}
                className="flex-1 rounded-lg bg-green-600 hover:bg-green-700 disabled:opacity-50 text-white text-sm font-medium py-2 transition-colors"
              >
                ✓ Accept
              </button>
              <button
                onClick={() => setEditMode(true)}
                disabled={isBusy}
                className="flex-1 rounded-lg bg-blue-600 hover:bg-blue-700 disabled:opacity-50 text-white text-sm font-medium py-2 transition-colors"
              >
                ✏️ Edit
              </button>
              <button
                onClick={() => reject.mutate(suggestion.id)}
                disabled={isBusy}
                className="flex-1 rounded-lg bg-red-100 hover:bg-red-200 disabled:opacity-50 text-red-700 text-sm font-medium py-2 transition-colors"
              >
                ✗ Reject
              </button>
            </>
          ) : (
            <>
              <button
                onClick={() => edit.mutate({ id: suggestion.id, editedResponse: editedText })}
                disabled={isBusy || !editedText.trim()}
                className="flex-1 rounded-lg bg-blue-600 hover:bg-blue-700 disabled:opacity-50 text-white text-sm font-medium py-2 transition-colors"
              >
                Save Edit
              </button>
              <button
                onClick={() => { setEditMode(false); setEditedText(suggestion.suggestedResponse) }}
                className="px-4 rounded-lg border border-gray-300 hover:bg-gray-50 text-sm font-medium py-2 transition-colors"
              >
                Cancel
              </button>
            </>
          )}
        </div>
      )}
    </div>
  )
}
