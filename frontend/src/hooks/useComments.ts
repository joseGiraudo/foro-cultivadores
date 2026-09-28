import { useQuery } from '@tanstack/react-query'
import { listComments } from '../api/comments'

export function useComments(postId: number) {
  return useQuery({
    queryKey: ['comments', postId],
    queryFn: () => listComments(postId),
  })
}
