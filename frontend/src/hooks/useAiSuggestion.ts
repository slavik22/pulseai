import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { aiApi } from '../api/ai'

export function useAiSuggestion(ticketId: string) {
  return useQuery({
    queryKey: ['ai-suggestion', ticketId],
    queryFn: () => aiApi.getSuggestionByTicket(ticketId),
    enabled: Boolean(ticketId),
    // WHY retry: 3 with delay?
    // AI generation takes time after ticket is created (Kafka async).
    // The suggestion might not exist yet when the agent opens the ticket.
    // Retry 3 times with exponential backoff before showing "not available".
    retry: 3,
    retryDelay: (attempt) => Math.min(1000 * 2 ** attempt, 10_000),
  })
}

export function useAcceptSuggestion() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (suggestionId: string) => aiApi.accept(suggestionId),
    onSuccess: (data) => {
      queryClient.setQueryData(['ai-suggestion', data.ticketId], data)
    },
  })
}

export function useRejectSuggestion() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (suggestionId: string) => aiApi.reject(suggestionId),
    onSuccess: (data) => {
      queryClient.setQueryData(['ai-suggestion', data.ticketId], data)
    },
  })
}

export function useEditSuggestion() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, editedResponse }: { id: string; editedResponse: string }) =>
      aiApi.edit(id, editedResponse),
    onSuccess: (data) => {
      queryClient.setQueryData(['ai-suggestion', data.ticketId], data)
    },
  })
}
