import {
  createColumnHelper,
  flexRender,
  getCoreRowModel,
  getSortedRowModel,
  useReactTable,
  type SortingState,
} from '@tanstack/react-table'
import { useState } from 'react'
import { formatDistanceToNow } from 'date-fns'
import { useNavigate } from 'react-router-dom'
import type { Priority, Ticket, TicketStatus } from '../../types'
import { clsx } from 'clsx'

/**
 * WHY TanStack Table?
 * Headless — brings zero CSS. We style it ourselves with Tailwind.
 * Handles: sorting, filtering, pagination, virtualization for large lists.
 * Stateful (controlled) or stateless (uncontrolled) — our choice.
 * VS. a UI library table: full control over markup + behavior.
 */

const columnHelper = createColumnHelper<Ticket>()

const PRIORITY_STYLES: Record<Priority, string> = {
  LOW: 'bg-gray-100 text-gray-700',
  MEDIUM: 'bg-blue-100 text-blue-700',
  HIGH: 'bg-orange-100 text-orange-700',
  CRITICAL: 'bg-red-100 text-red-700',
}

const STATUS_STYLES: Record<TicketStatus, string> = {
  OPEN: 'bg-yellow-100 text-yellow-800',
  IN_PROGRESS: 'bg-blue-100 text-blue-800',
  RESOLVED: 'bg-green-100 text-green-800',
  CLOSED: 'bg-gray-100 text-gray-600',
}

const columns = [
  columnHelper.accessor('title', {
    header: 'Title',
    cell: (info) => (
      <span className="font-medium text-gray-900 line-clamp-1">{info.getValue()}</span>
    ),
  }),
  columnHelper.accessor('status', {
    header: 'Status',
    cell: (info) => (
      <span className={clsx('rounded-full px-2 py-0.5 text-xs font-semibold', STATUS_STYLES[info.getValue()])}>
        {info.getValue().replace('_', ' ')}
      </span>
    ),
  }),
  columnHelper.accessor('priority', {
    header: 'Priority',
    cell: (info) => (
      <span className={clsx('rounded-full px-2 py-0.5 text-xs font-semibold', PRIORITY_STYLES[info.getValue()])}>
        {info.getValue()}
      </span>
    ),
  }),
  columnHelper.accessor('category', {
    header: 'Category',
    cell: (info) => <span className="text-gray-500 text-sm">{info.getValue() ?? '—'}</span>,
  }),
  columnHelper.accessor('assignedAgentName', {
    header: 'Assigned To',
    cell: (info) => (
      <span className="text-sm text-gray-600">{info.getValue() ?? 'Unassigned'}</span>
    ),
  }),
  columnHelper.accessor('createdAt', {
    header: 'Created',
    cell: (info) => (
      <span className="text-sm text-gray-500">
        {formatDistanceToNow(new Date(info.getValue()), { addSuffix: true })}
      </span>
    ),
  }),
]

interface Props {
  tickets: Ticket[]
  isLoading: boolean
}

export function TicketList({ tickets, isLoading }: Props) {
  const navigate = useNavigate()
  const [sorting, setSorting] = useState<SortingState>([])

  const table = useReactTable({
    data: tickets,
    columns,
    state: { sorting },
    onSortingChange: setSorting,
    getCoreRowModel: getCoreRowModel(),
    getSortedRowModel: getSortedRowModel(),
  })

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-48">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600" />
      </div>
    )
  }

  return (
    <div className="overflow-x-auto rounded-lg border border-gray-200">
      <table className="min-w-full divide-y divide-gray-200">
        <thead className="bg-gray-50">
          {table.getHeaderGroups().map((hg) => (
            <tr key={hg.id}>
              {hg.headers.map((header) => (
                <th
                  key={header.id}
                  className={clsx(
                    'px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider',
                    header.column.getCanSort() && 'cursor-pointer select-none hover:bg-gray-100'
                  )}
                  onClick={header.column.getToggleSortingHandler()}
                >
                  {flexRender(header.column.columnDef.header, header.getContext())}
                  {header.column.getIsSorted() === 'asc' ? ' ↑' : header.column.getIsSorted() === 'desc' ? ' ↓' : ''}
                </th>
              ))}
            </tr>
          ))}
        </thead>
        <tbody className="bg-white divide-y divide-gray-100">
          {table.getRowModel().rows.map((row) => (
            <tr
              key={row.id}
              className="hover:bg-blue-50 cursor-pointer transition-colors"
              onClick={() => navigate(`/tickets/${row.original.id}`)}
            >
              {row.getVisibleCells().map((cell) => (
                <td key={cell.id} className="px-4 py-3">
                  {flexRender(cell.column.columnDef.cell, cell.getContext())}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
      {tickets.length === 0 && (
        <div className="text-center py-12 text-gray-400">No tickets found.</div>
      )}
    </div>
  )
}
