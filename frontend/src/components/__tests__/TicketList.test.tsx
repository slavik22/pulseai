import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { TicketList } from '../tickets/TicketList'

const mockTickets = [
  { id: '1', title: 'Login issue', description: 'Cannot login', status: 'OPEN' as const, priority: 'HIGH' as const, createdAt: new Date().toISOString() },
]

describe('TicketList', () => {
  it('renders ticket titles', () => {
    render(<TicketList tickets={mockTickets} onSelect={vi.fn()} />)
    expect(screen.getByText('Login issue')).toBeInTheDocument()
  })

  it('renders empty state when no tickets', () => {
    render(<TicketList tickets={[]} onSelect={vi.fn()} />)
    expect(screen.getByText(/no tickets/i)).toBeInTheDocument()
  })

  it('renders priority badge', () => {
    render(<TicketList tickets={mockTickets} onSelect={vi.fn()} />)
    expect(screen.getByText('HIGH')).toBeInTheDocument()
  })
})
