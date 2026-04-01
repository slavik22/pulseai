import { apiClient } from './client'
import type { AssignTicketPayload, CreateTicketPayload, ResolveTicketPayload, Ticket } from '../types'

export const ticketsApi = {
  getById: (id: string) =>
    apiClient.get<Ticket>(`/api/v1/tickets/${id}`).then((r) => r.data),

  getByCustomer: (customerId: string) =>
    apiClient.get<Ticket[]>('/api/v1/tickets', { params: { customerId } }).then((r) => r.data),

  getByAgent: (agentId: string) =>
    apiClient.get<Ticket[]>(`/api/v1/tickets/agent/${agentId}`).then((r) => r.data),

  create: (payload: CreateTicketPayload) =>
    apiClient.post<Ticket>('/api/v1/tickets', payload).then((r) => r.data),

  assign: (ticketId: string, payload: AssignTicketPayload) =>
    apiClient.patch<Ticket>(`/api/v1/tickets/${ticketId}/assign`, payload).then((r) => r.data),

  resolve: (ticketId: string, payload: ResolveTicketPayload) =>
    apiClient.patch<Ticket>(`/api/v1/tickets/${ticketId}/resolve`, payload).then((r) => r.data),
}
