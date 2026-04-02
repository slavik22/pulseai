import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { aiApi } from '../api/ai'

export function useKnowledgeArticles() {
  return useQuery({
    queryKey: ['knowledge-articles'],
    queryFn: () => aiApi.getArticles(),
    staleTime: 60_000,
  })
}

export function useCreateArticle() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ title, content, category }: { title: string; content: string; category: string }) =>
      aiApi.createArticle(title, content, category),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['knowledge-articles'] })
    },
  })
}
