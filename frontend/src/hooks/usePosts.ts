import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { getPost, listPosts, type ListPostsParams } from '../api/posts'

export function usePosts(params: ListPostsParams) {
  return useQuery({
    queryKey: ['posts', params],
    queryFn: () => listPosts(params),
    // Mantener la página anterior visible mientras carga la nueva (paginación/filtros).
    placeholderData: keepPreviousData,
  })
}

export function usePost(id: number) {
  return useQuery({
    queryKey: ['post', id],
    queryFn: () => getPost(id),
  })
}
