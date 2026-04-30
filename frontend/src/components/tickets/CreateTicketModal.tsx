import { useState } from 'react'
import { Modal } from '../ui/Modal'
import { useCreateTicket } from '../../hooks/useTickets'
import { useAuthStore } from '../../store/authStore'
import type { Priority } from '../../types'
import { clsx } from 'clsx'

interface Props {
  open: boolean
  onClose: () => void
}

const PRIORITIES: Priority[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']

const PRIORITY_COLORS: Record<Priority, string> = {
  LOW: 'border-slate-300 text-slate-600 bg-slate-50',
  MEDIUM: 'border-sky-300 text-sky-700 bg-sky-50',
  HIGH: 'border-orange-300 text-orange-700 bg-orange-50',
  CRITICAL: 'border-rose-300 text-rose-700 bg-rose-50',
}

const PRIORITY_SELECTED: Record<Priority, string> = {
  LOW: 'border-slate-500 bg-slate-100 ring-2 ring-slate-400',
  MEDIUM: 'border-sky-500 bg-sky-100 ring-2 ring-sky-400',
  HIGH: 'border-orange-500 bg-orange-100 ring-2 ring-orange-400',
  CRITICAL: 'border-rose-500 bg-rose-100 ring-2 ring-rose-400',
}

const CATEGORIES = ['auth', 'billing', 'performance', 'bug', 'feature', 'other']

export function CreateTicketModal({ open, onClose }: Props) {
  const agent = useAuthStore((s) => s.agent)
  const createTicket = useCreateTicket()

  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')
  const [priority, setPriority] = useState<Priority>('MEDIUM')
  const [category, setCategory] = useState('other')
  const [error, setError] = useState<string | null>(null)

  const reset = () => {
    setTitle('')
    setContent('')
    setPriority('MEDIUM')
    setCategory('other')
    setError(null)
  }

  const handleClose = () => {
    reset()
    onClose()
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)

    if (!title.trim() || !content.trim()) {
      setError('Title and description are required.')
      return
    }

    try {
      await createTicket.mutateAsync({
        title: title.trim(),
        content: content.trim(),
        priority,
        customerId: agent?.id ?? 'anonymous',
        category,
      })
      handleClose()
    } catch (err: any) {
      setError(err?.detail ?? 'Failed to create ticket. Please try again.')
    }
  }

  return (
    <Modal open={open} onClose={handleClose} title="Create New Ticket" size="lg">
      <form onSubmit={handleSubmit} className="space-y-5">

        {/* Title */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1.5">
            Title <span className="text-rose-500">*</span>
          </label>
          <input
            type="text"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="Brief summary of the issue"
            maxLength={200}
            className="w-full rounded-xl border border-gray-200 px-4 py-2.5 text-sm text-gray-900 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-all"
          />
          <p className="text-xs text-gray-400 mt-1 text-right">{title.length}/200</p>
        </div>

        {/* Priority */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Priority <span className="text-rose-500">*</span>
          </label>
          <div className="grid grid-cols-4 gap-2">
            {PRIORITIES.map((p) => (
              <button
                key={p}
                type="button"
                onClick={() => setPriority(p)}
                className={clsx(
                  'py-2 rounded-xl border text-xs font-semibold transition-all',
                  priority === p ? PRIORITY_SELECTED[p] : PRIORITY_COLORS[p] + ' hover:opacity-80'
                )}
              >
                {p}
              </button>
            ))}
          </div>
        </div>

        {/* Category */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1.5">Category</label>
          <div className="flex flex-wrap gap-2">
            {CATEGORIES.map((cat) => (
              <button
                key={cat}
                type="button"
                onClick={() => setCategory(cat)}
                className={clsx(
                  'px-3 py-1.5 rounded-lg text-xs font-medium border transition-all capitalize',
                  category === cat
                    ? 'bg-indigo-600 text-white border-indigo-600'
                    : 'bg-white text-gray-600 border-gray-200 hover:border-indigo-300 hover:text-indigo-600'
                )}
              >
                {cat}
              </button>
            ))}
          </div>
        </div>

        {/* Description */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1.5">
            Description <span className="text-rose-500">*</span>
          </label>
          <textarea
            value={content}
            onChange={(e) => setContent(e.target.value)}
            placeholder="Describe the issue in detail. Include steps to reproduce, error messages, or screenshots if applicable."
            rows={5}
            className="w-full rounded-xl border border-gray-200 px-4 py-2.5 text-sm text-gray-900 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-all resize-none"
          />
        </div>

        {/* Error */}
        {error && (
          <div className="rounded-xl bg-rose-50 border border-rose-200 px-4 py-3 text-sm text-rose-700">
            {error}
          </div>
        )}

        {/* Actions */}
        <div className="flex gap-3 pt-1">
          <button
            type="button"
            onClick={handleClose}
            className="flex-1 rounded-xl border border-gray-200 py-2.5 text-sm font-medium text-gray-600 hover:bg-gray-50 transition-colors"
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={createTicket.isPending}
            className="flex-1 rounded-xl bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50 text-white py-2.5 text-sm font-semibold transition-colors shadow-sm"
          >
            {createTicket.isPending ? 'Creating…' : 'Create Ticket'}
          </button>
        </div>
      </form>
    </Modal>
  )
}
