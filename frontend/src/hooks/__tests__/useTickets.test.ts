import { describe, it, expect, vi, beforeEach } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'
import { useTickets } from '../useTickets'
import * as ticketsApi from '../../api/tickets'

vi.mock('../../api/tickets')

const mockTickets = [
  { id: '1', title: 'Login issue', description: 'Cannot login', status: 'OPEN', priority: 'HIGH', createdAt: new Date().toISOString() },
  { id: '2', title: 'DB error', description: 'DB down', status: 'IN_PROGRESS', priority: 'CRITICAL', createdAt: new Date().toISOString() },
]

describe('useTickets', () => {
  beforeEach(() => {
    vi.mocked(ticketsApi.fetchTickets).mockResolvedValue(mockTickets)
  })

  it('should return tickets on successful fetch', async () => {
    const { result } = renderHook(() => useTickets())
    expect(result.current.loading).toBe(true)
    await waitFor(() => expect(result.current.loading).toBe(false))
    expect(result.current.tickets).toHaveLength(2)
    expect(result.current.error).toBeNull()
  })

  it('should handle fetch error', async () => {
    vi.mocked(ticketsApi.fetchTickets).mockRejectedValue(new Error('Network error'))
    const { result } = renderHook(() => useTickets())
    await waitFor(() => expect(result.current.loading).toBe(false))
    expect(result.current.error).toBeTruthy()
    expect(result.current.tickets).toHaveLength(0)
  })
})
