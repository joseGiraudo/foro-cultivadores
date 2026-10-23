import { api, type QueryParams } from './client'
import type { Page, PostResponse, PostSummaryResponse } from './types'

export interface ListPostsParams {
  categoryIds?: number[] // unión/OR; el backend prioriza los posts que matchean más categorías
  authorId?: number
  postType?: string
  status?: string // solo aplica/es visible para MODERATOR+
  q?: string // búsqueda full-text (título + cuerpo); ordena por relevancia en el backend
  following?: boolean // feed "siguiendo": solo posts de autores que el usuario sigue
  page?: number
  size?: number
  sort?: string
}

// GET /posts → página de resúmenes. Orden por defecto createdAt desc (lo pone el backend).
export function listPosts(params: ListPostsParams = {}): Promise<Page<PostSummaryResponse>> {
  // Mandar q, following y categoryIds solo cuando aplican (no ensuciar la URL ni la query key).
  const { q, following, categoryIds, ...rest } = params
  const query: ListPostsParams = { ...rest }
  if (q && q.trim()) query.q = q.trim()
  if (following) query.following = true
  if (categoryIds && categoryIds.length > 0) query.categoryIds = categoryIds
  return api.get<Page<PostSummaryResponse>>('/posts', query as QueryParams)
}

// GET /posts/{id} → detalle. 404 si no existe o no es visible para quien consulta.
export function getPost(id: number): Promise<PostResponse> {
  return api.get<PostResponse>(`/posts/${id}`)
}

export interface CreatePostBody {
  title: string
  content: string
  postType: string
  categoryIds: number[]
}

// POST /posts → 201 con el post creado. 403 si el rol no alcanza para el tipo elegido.
export function createPost(body: CreatePostBody): Promise<PostResponse> {
  return api.post<PostResponse>('/posts', body)
}

// PATCH /posts/{id} → edición parcial (title/content/categoryIds). Solo autor, solo si ACTIVE.
// El postType no se puede cambiar.
export interface UpdatePostBody {
  title?: string
  content?: string
  categoryIds?: number[]
}

export function updatePost(id: number, body: UpdatePostBody): Promise<PostResponse> {
  return api.patch<PostResponse>(`/posts/${id}`, body)
}

// PATCH /posts/{id}/status → cambia el estado (autor: DELETED_BY_USER; moderador: HIDDEN/ACTIVE/DELETED_BY_MOD).
export function changePostStatus(id: number, status: string): Promise<PostResponse> {
  return api.patch<PostResponse>(`/posts/${id}/status`, { status })
}

// POST /posts/{id}/save → guarda el post (204). Idempotente.
export function savePost(id: number): Promise<void> {
  return api.post<void>(`/posts/${id}/save`)
}

// DELETE /posts/{id}/save → quita el post de guardados (204). Idempotente.
export function unsavePost(id: number): Promise<void> {
  return api.delete<void>(`/posts/${id}/save`)
}

// GET /me/saved-posts → página de posts guardados por el usuario, del más reciente al más viejo.
export function listSavedPosts(page = 0): Promise<Page<PostSummaryResponse>> {
  return api.get<Page<PostSummaryResponse>>('/me/saved-posts', { page })
}
