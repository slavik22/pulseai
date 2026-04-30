import { apiClient } from './client'
import type { AiSuggestion, KnowledgeArticle } from '../types'

export const aiApi = {
  getSuggestionByTicket: (ticketId: string) =>
    apiClient
      .get<AiSuggestion>(`/api/v1/ai/suggestions/ticket/${ticketId}`)
      .then((r) => r.data),

  accept: (suggestionId: string) =>
    apiClient
      .post<AiSuggestion>(`/api/v1/ai/suggestions/${suggestionId}/accept`)
      .then((r) => r.data),

  reject: (suggestionId: string) =>
    apiClient
      .post<AiSuggestion>(`/api/v1/ai/suggestions/${suggestionId}/reject`)
      .then((r) => r.data),

  edit: (suggestionId: string, editedResponse: string) =>
    apiClient
      .post<AiSuggestion>(`/api/v1/ai/suggestions/${suggestionId}/edit`, { editedResponse })
      .then((r) => r.data),

  getArticles: () =>
    apiClient.get<KnowledgeArticle[]>('/api/v1/ai/articles').then((r) => r.data),

  createArticle: (title: string, content: string, category: string) =>
    apiClient.post('/api/v1/ai/articles', { title, content, category }),
}
