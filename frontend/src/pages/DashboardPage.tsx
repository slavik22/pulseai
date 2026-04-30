import { useState } from 'react'
import { useTicketsByAgent } from '../hooks/useTickets'
import { useAuthStore } from '../store/authStore'
import { useNotifications } from '../hooks/useNotifications'
import { TicketList } from '../components/tickets/TicketList'
import { CreateTicketModal } from '../components/tickets/CreateTicketModal'
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Cell } from 'recharts'
import type { Ticket } from '../types'

const PRIORITY_COLORS = {
  LOW: '#94a3b8',
  MEDIUM: '#38bdf8',
  HIGH: '#f97316',
  CRITICAL: '#ef4444',
}

function buildPriorityStats(tickets: Ticket[]) {
  const counts = { LOW: 0, MEDIUM: 0, HIGH: 0, CRITICAL: 0 }
  tickets.forEach((t) => { counts[t.priority]++ })
  return Object.entries(counts).map(([priority, count]) => ({ priority, count }))
}

interface StatCardProps {
  label: string
  value: number
  icon: React.ReactNode
  colorClass: string
  bgClass: string
}

function StatCard({ label, value, icon, colorClass, bgClass }: StatCardProps) {
  return (
    <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-5 flex items-center gap-4">
      <div className={`w-12 h-12 rounded-xl ${bgClass} flex items-center justify-center`}>
        <div className={colorClass}>{icon}</div>
      </div>
      <div>
        <p className="text-sm text-gray-500 font-medium">{label}</p>
        <p className="text-3xl font-bold text-gray-900">{value}</p>
      </div>
    </div>
  )
}

export function DashboardPage() {
  const agent = useAuthStore((s) => s.agent)
  const { data: tickets = [], isLoading, error } = useTicketsByAgent(agent?.id ?? '')
  const [createOpen, setCreateOpen] = useState(false)

  useNotifications()

  const open = tickets.filter((t) => t.status === 'OPEN')
  const inProgress = tickets.filter((t) => t.status === 'IN_PROGRESS')
  const resolved = tickets.filter((t) => t.status === 'RESOLVED')
  const critical = tickets.filter((t) => t.priority === 'CRITICAL' && t.status !== 'RESOLVED')
  const priorityStats = buildPriorityStats(tickets)

  return (
    <div className="p-8 max-w-7xl">
      <CreateTicketModal open={createOpen} onClose={() => setCreateOpen(false)} />

      {/* Header */}
      <div className="flex items-start justify-between mb-8">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">
            Good {getTimeOfDay()}, {agent?.name?.split(' ')[0] ?? 'Agent'} 👋
          </h1>
          <p className="text-gray-500 mt-1 text-sm">
            Here's your ticket queue for today
          </p>
        </div>
        <button
          onClick={() => setCreateOpen(true)}
          className="flex items-center gap-2 bg-indigo-600 hover:bg-indigo-700 text-white px-5 py-2.5 rounded-xl text-sm font-semibold transition-colors shadow-sm shadow-indigo-200"
        >
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
          </svg>
          New Ticket
        </button>
      </div>

      {/* Critical alert */}
      {critical.length > 0 && (
        <div className="mb-6 rounded-2xl bg-rose-50 border border-rose-200 px-5 py-4 flex items-center gap-3 animate-fade-in">
          <div className="w-8 h-8 rounded-full bg-rose-100 flex items-center justify-center shrink-0">
            <svg className="w-4 h-4 text-rose-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
            </svg>
          </div>
          <p className="text-sm font-medium text-rose-800">
            {critical.length} critical ticket{critical.length > 1 ? 's' : ''} require immediate attention
          </p>
        </div>
      )}

      {/* Stats */}
      <div className="grid grid-cols-2 xl:grid-cols-4 gap-4 mb-6">
        <StatCard
          label="Open"
          value={open.length}
          bgClass="bg-amber-50"
          colorClass="text-amber-500"
          icon={<svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4" /></svg>}
        />
        <StatCard
          label="In Progress"
          value={inProgress.length}
          bgClass="bg-blue-50"
          colorClass="text-blue-500"
          icon={<svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" /></svg>}
        />
        <StatCard
          label="Resolved"
          value={resolved.length}
          bgClass="bg-emerald-50"
          colorClass="text-emerald-500"
          icon={<svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>}
        />
        <StatCard
          label="Total"
          value={tickets.length}
          bgClass="bg-indigo-50"
          colorClass="text-indigo-500"
          icon={<svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" /></svg>}
        />
      </div>

      {/* Chart + Ticket list */}
      <div className="grid grid-cols-1 xl:grid-cols-3 gap-6">
        {/* Priority chart */}
        <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-5">
          <h2 className="text-sm font-semibold text-gray-500 uppercase tracking-wider mb-5">By Priority</h2>
          <ResponsiveContainer width="100%" height={200}>
            <BarChart data={priorityStats} barSize={36}>
              <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
              <XAxis dataKey="priority" axisLine={false} tickLine={false} tick={{ fontSize: 11, fill: '#94a3b8' }} />
              <YAxis allowDecimals={false} axisLine={false} tickLine={false} tick={{ fontSize: 11, fill: '#94a3b8' }} />
              <Tooltip
                cursor={{ fill: '#f8fafc' }}
                contentStyle={{ borderRadius: '12px', border: '1px solid #e2e8f0', fontSize: '12px' }}
              />
              <Bar dataKey="count" radius={[6, 6, 0, 0]}>
                {priorityStats.map((entry) => (
                  <Cell key={entry.priority} fill={PRIORITY_COLORS[entry.priority as keyof typeof PRIORITY_COLORS]} />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </div>

        {/* Ticket list */}
        <div className="xl:col-span-2">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">My Tickets</h2>
          {error ? (
            <div className="rounded-2xl bg-rose-50 border border-rose-200 px-5 py-4 text-sm text-rose-700">
              Failed to load tickets. Please refresh.
            </div>
          ) : (
            <TicketList tickets={tickets} isLoading={isLoading} />
          )}
        </div>
      </div>
    </div>
  )
}

function getTimeOfDay() {
  const h = new Date().getHours()
  if (h < 12) return 'morning'
  if (h < 17) return 'afternoon'
  return 'evening'
}
