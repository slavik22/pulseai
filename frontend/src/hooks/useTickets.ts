import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ticketsApi } from '../api/tickets'
import type { AssignTicketPayload, CreateTicketPayload, ResolveTicketPayload } from '../types'

/**
 * WHY TanStack Query (React Query)?
 *
 * Data fetching involves: loading state, error state, caching, refetching,
 * background updates, optimistic updates, deduplication of parallel requests.
 * Writing this with useState + useEffect = hundreds of lines of subtle bugs.
 *
 * React Query gives all of this for free with a clean API.
 *
 * WHY custom hooks (useTickets, useTicket) over calling useQuery directly in components?
 * Components should not know about API details (URLs, query keys, cache config).
 * Components call useTickets() and get {tickets, isLoading, error}.
 * Changing the API? Change the hook. Components untouched.
 * This is the same Dependency Inversion principle from the backend — applied to React.
 *
 * WHY queryKey arrays like ['tickets', 'by-agent', agentId]?
 * React Query uses these as cache keys. Hierarchical: invalidating ['tickets']
 * invalidates all ticket queries. Invalidating ['tickets', 'by-agent', 'a1']
 * only invalidates that specific agent's list.
 */

export function useTicketsByAgent(agentId: string) {
  return useQuery({
    queryKey: ['tickets', 'by-agent', agentId],
    queryFn: () => ticketsApi.getByAgent(agentId),
    enabled: Boolean(agentId),
    // WHY refetchInterval? Dashboard stays fresh without manual refresh.
    // Polls every 30s — lightweight enough for a dashboard, fresh enough for agents.
    refetchInterval: 30_000,
    staleTime: 10_000,
  })
}

export function useTicket(ticketId: string) {
  return useQuery({
    queryKey: ['tickets', ticketId],
    queryFn: () => ticketsApi.getById(ticketId),
    enabled: Boolean(ticketId),
  })
}

export function useCreateTicket() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (payload: CreateTicketPayload) => ticketsApi.create(payload),
    onSuccess: () => {
      // Invalidate all ticket list queries — new ticket should appear everywhere
      queryClient.invalidateQueries({ queryKey: ['tickets'] })
    },
  })
}

export function useAssignTicket() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ ticketId, payload }: { ticketId: string; payload: AssignTicketPayload }) =>
      ticketsApi.assign(ticketId, payload),

    /**
     * WHY optimistic update?
     * Without it: agent clicks "Assign" → UI shows stale "OPEN" → server responds →
     * UI updates. That lag feels slow and broken.
     *
     * With optimistic update: UI updates INSTANTLY to IN_PROGRESS.
     * If the server rejects it (e.g. ticket already closed) → rollback to previous state.
     * The result: the UI feels instant even on slow networks.
     */
    onMutate: async ({ ticketId, payload }) => {
      await queryClient.cancelQueries({ queryKey: ['tickets', ticketId] })
      const previous = queryClient.getQueryData(['tickets', ticketId])

      queryClient.setQueryData(['tickets', ticketId], (old: any) =>
        old
          ? {
              ...old,
              status: 'IN_PROGRESS',
              assignedAgentId: payload.agentId,
              assignedAgentName: payload.agentName,
            }
          : old
      )
      return { previous }
    },
    onError: (_err, { ticketId }, ctx) => {
      // Rollback on failure
      if (ctx?.previous) queryClient.setQueryData(['tickets', ticketId], ctx.previous)
    },
    onSettled: (_data, _err, { ticketId }) => {
      // Always refetch after mutation to ensure server state is reflected
      queryClient.invalidateQueries({ queryKey: ['tickets', ticketId] })
      queryClient.invalidateQueries({ queryKey: ['tickets'] })
    },
  })
}

export function useResolveTicket() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ ticketId, payload }: { ticketId: string; payload: ResolveTicketPayload }) =>
      ticketsApi.resolve(ticketId, payload),
    onSuccess: (_data, { ticketId }) => {
      queryClient.invalidateQueries({ queryKey: ['tickets', ticketId] })
      queryClient.invalidateQueries({ queryKey: ['tickets'] })
    },
  })
}
