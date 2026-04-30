import { useState } from 'react'
import { formatDistanceToNow } from 'date-fns'
import { useKnowledgeArticles, useCreateArticle } from '../hooks/useKnowledge'
import { Modal } from '../components/ui/Modal'
import type { KnowledgeArticle } from '../types'
import { clsx } from 'clsx'

const CATEGORIES = ['auth', 'billing', 'performance', 'bug', 'feature', 'general', 'other']

function ArticleCard({ article }: { article: KnowledgeArticle }) {
  const [expanded, setExpanded] = useState(false)

  return (
    <div className="bg-white rounded-2xl border border-gray-100 shadow-sm hover:shadow-md transition-all duration-200 overflow-hidden">
      <div className="p-5">
        <div className="flex items-start justify-between gap-3 mb-3">
          <h3 className="font-semibold text-gray-900 text-sm leading-snug flex-1">{article.title}</h3>
          <div className="flex items-center gap-2 shrink-0">
            {article.indexed ? (
              <span className="inline-flex items-center gap-1 bg-emerald-50 text-emerald-700 text-xs font-medium px-2.5 py-1 rounded-full border border-emerald-200">
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-500" />
                Indexed
              </span>
            ) : (
              <span className="inline-flex items-center gap-1 bg-amber-50 text-amber-700 text-xs font-medium px-2.5 py-1 rounded-full border border-amber-200">
                <span className="w-1.5 h-1.5 rounded-full bg-amber-500" />
                Pending
              </span>
            )}
          </div>
        </div>

        <div className="flex items-center gap-2 mb-3">
          {article.category && (
            <span className="bg-indigo-50 text-indigo-600 text-xs font-medium px-2.5 py-1 rounded-lg capitalize">
              {article.category}
            </span>
          )}
          {article.verifiedByExpert && (
            <span className="bg-violet-50 text-violet-600 text-xs font-medium px-2.5 py-1 rounded-lg">
              ✓ Expert verified
            </span>
          )}
        </div>

        <p className={clsx('text-sm text-gray-500 leading-relaxed', !expanded && 'line-clamp-2')}>
          {article.content}
        </p>

        {article.content.length > 120 && (
          <button
            onClick={() => setExpanded(!expanded)}
            className="text-xs text-indigo-600 hover:text-indigo-700 font-medium mt-1.5 transition-colors"
          >
            {expanded ? 'Show less' : 'Read more'}
          </button>
        )}
      </div>

      <div className="px-5 py-3 bg-gray-50 border-t border-gray-100 flex items-center justify-between">
        <span className="text-xs text-gray-400">
          Added {formatDistanceToNow(new Date(article.createdAt), { addSuffix: true })}
        </span>
        {article.indexedAt && (
          <span className="text-xs text-gray-400">
            Indexed {formatDistanceToNow(new Date(article.indexedAt), { addSuffix: true })}
          </span>
        )}
      </div>
    </div>
  )
}

function AddArticleModal({ open, onClose }: { open: boolean; onClose: () => void }) {
  const createArticle = useCreateArticle()
  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')
  const [category, setCategory] = useState('general')
  const [error, setError] = useState<string | null>(null)

  const reset = () => {
    setTitle('')
    setContent('')
    setCategory('general')
    setError(null)
  }

  const handleClose = () => { reset(); onClose() }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)

    if (!title.trim()) { setError('Title is required.'); return }
    if (content.trim().length < 50) { setError('Content must be at least 50 characters.'); return }

    try {
      await createArticle.mutateAsync({ title: title.trim(), content: content.trim(), category })
      handleClose()
    } catch (err: any) {
      setError(err?.detail ?? 'Failed to create article.')
    }
  }

  return (
    <Modal open={open} onClose={handleClose} title="Add Knowledge Article" size="lg">
      <form onSubmit={handleSubmit} className="space-y-5">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1.5">
            Title <span className="text-rose-500">*</span>
          </label>
          <input
            type="text"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="e.g. Login Troubleshooting Guide"
            className="w-full rounded-xl border border-gray-200 px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-all"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">Category</label>
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

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1.5">
            Content <span className="text-rose-500">*</span>
          </label>
          <textarea
            value={content}
            onChange={(e) => setContent(e.target.value)}
            placeholder="Provide detailed information. This content will be embedded into the vector database and used by the AI to generate support responses. Minimum 50 characters."
            rows={7}
            className="w-full rounded-xl border border-gray-200 px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-all resize-none"
          />
          <p className={clsx('text-xs mt-1 text-right', content.length < 50 ? 'text-amber-500' : 'text-gray-400')}>
            {content.length} chars {content.length < 50 && `(${50 - content.length} more needed)`}
          </p>
        </div>

        <div className="rounded-xl bg-violet-50 border border-violet-200 px-4 py-3 text-xs text-violet-700 flex gap-2">
          <span className="shrink-0">✨</span>
          <span>This article will be split into chunks, embedded with OpenAI, and stored in pgvector. It will immediately improve AI suggestion quality for related tickets.</span>
        </div>

        {error && (
          <div className="rounded-xl bg-rose-50 border border-rose-200 px-4 py-3 text-sm text-rose-700">{error}</div>
        )}

        <div className="flex gap-3 pt-1">
          <button type="button" onClick={handleClose}
            className="flex-1 rounded-xl border border-gray-200 py-2.5 text-sm font-medium text-gray-600 hover:bg-gray-50 transition-colors">
            Cancel
          </button>
          <button type="submit" disabled={createArticle.isPending}
            className="flex-1 rounded-xl bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50 text-white py-2.5 text-sm font-semibold transition-colors shadow-sm">
            {createArticle.isPending ? 'Indexing…' : 'Add & Index Article'}
          </button>
        </div>
      </form>
    </Modal>
  )
}

export function KnowledgePage() {
  const { data: articles = [], isLoading } = useKnowledgeArticles()
  const [modalOpen, setModalOpen] = useState(false)
  const [search, setSearch] = useState('')
  const [filterCategory, setFilterCategory] = useState<string>('all')

  const filtered = articles.filter((a) => {
    const matchSearch = !search || a.title.toLowerCase().includes(search.toLowerCase()) ||
      a.content.toLowerCase().includes(search.toLowerCase())
    const matchCat = filterCategory === 'all' || a.category === filterCategory
    return matchSearch && matchCat
  })

  const indexed = articles.filter((a) => a.indexed).length

  return (
    <div className="p-8">
      <AddArticleModal open={modalOpen} onClose={() => setModalOpen(false)} />

      {/* Header */}
      <div className="flex items-start justify-between mb-8">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Knowledge Base</h1>
          <p className="text-gray-500 mt-1 text-sm">
            {articles.length} articles · {indexed} indexed for AI
          </p>
        </div>
        <button
          onClick={() => setModalOpen(true)}
          className="flex items-center gap-2 bg-indigo-600 hover:bg-indigo-700 text-white px-5 py-2.5 rounded-xl text-sm font-semibold transition-colors shadow-sm shadow-indigo-200"
        >
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
          </svg>
          Add Article
        </button>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-3 gap-4 mb-6">
        {[
          { label: 'Total Articles', value: articles.length, color: 'text-indigo-600', bg: 'bg-indigo-50' },
          { label: 'Indexed for AI', value: indexed, color: 'text-emerald-600', bg: 'bg-emerald-50' },
          { label: 'Pending Index', value: articles.length - indexed, color: 'text-amber-600', bg: 'bg-amber-50' },
        ].map((s) => (
          <div key={s.label} className={`${s.bg} rounded-2xl p-4 border border-white/80`}>
            <p className="text-xs font-medium text-gray-500 mb-1">{s.label}</p>
            <p className={`text-3xl font-bold ${s.color}`}>{s.value}</p>
          </div>
        ))}
      </div>

      {/* Filters */}
      <div className="flex gap-3 mb-6">
        <div className="relative flex-1 max-w-sm">
          <svg className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search articles…"
            className="w-full pl-9 pr-4 py-2.5 rounded-xl border border-gray-200 bg-white text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-all"
          />
        </div>
        <div className="flex gap-2">
          {['all', ...CATEGORIES].map((cat) => (
            <button
              key={cat}
              onClick={() => setFilterCategory(cat)}
              className={clsx(
                'px-3 py-2 rounded-xl text-xs font-medium border transition-all capitalize',
                filterCategory === cat
                  ? 'bg-indigo-600 text-white border-indigo-600'
                  : 'bg-white text-gray-600 border-gray-200 hover:border-indigo-300'
              )}
            >
              {cat}
            </button>
          ))}
        </div>
      </div>

      {/* Article grid */}
      {isLoading ? (
        <div className="flex items-center justify-center h-48">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600" />
        </div>
      ) : filtered.length === 0 ? (
        <div className="text-center py-20 bg-white rounded-2xl border border-gray-100">
          <div className="w-16 h-16 rounded-2xl bg-indigo-50 flex items-center justify-center mx-auto mb-4">
            <svg className="w-8 h-8 text-indigo-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5}
                d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
            </svg>
          </div>
          <p className="text-gray-500 font-medium mb-1">No articles found</p>
          <p className="text-gray-400 text-sm mb-5">
            {articles.length === 0 ? 'Add your first article to start improving AI suggestions.' : 'Try adjusting your search or filter.'}
          </p>
          {articles.length === 0 && (
            <button onClick={() => setModalOpen(true)}
              className="bg-indigo-600 hover:bg-indigo-700 text-white px-5 py-2.5 rounded-xl text-sm font-semibold transition-colors">
              Add First Article
            </button>
          )}
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
          {filtered.map((article) => (
            <ArticleCard key={article.id} article={article} />
          ))}
        </div>
      )}
    </div>
  )
}
