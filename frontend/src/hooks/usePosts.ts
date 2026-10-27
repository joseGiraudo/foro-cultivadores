import { keepPreviousData, useQuery } from '@tanstack/react-query'
import {
  getPost,
  listCreatableTypes,
  listPosts,
  listSavedPosts,
  type ListPostsParams,
} from '../api/posts'

export function usePosts(params: ListPostsParams, options?: { enabled?: boolean }) {
  return useQuery({
    queryKey: ['posts', params],
    queryFn: () => listPosts(params),
    // Mantener la página anterior visible mientras carga la nueva (paginación/filtros).
    placeholderData: keepPreviousData,
    // Permite esperar a que otro dato esté listo (p. ej. el id del perfil por username).
    enabled: options?.enabled ?? true,
  })
}

export function usePost(id: number) {
  return useQuery({
    queryKey: ['post', id],
    queryFn: () => getPost(id),
  })
}

// Tipos de post que el usuario actual puede crear (según su rol). Solo tiene sentido logueado.
export function useCreatableTypes(options?: { enabled?: boolean }) {
  return useQuery({
    queryKey: ['creatableTypes'],
    queryFn: listCreatableTypes,
    enabled: options?.enabled ?? true,
  })
}

export function useSavedPosts(page: number) {
  return useQuery({
    queryKey: ['savedPosts', page],
    queryFn: () => listSavedPosts(page),
    placeholderData: keepPreviousData,
  })
}
