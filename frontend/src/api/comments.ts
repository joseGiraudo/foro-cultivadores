import { api } from './client'
import type { CommentResponse } from './types'

// GET /posts/{postId}/comments → lista plana en recorrido en profundidad (ver mvp-spec §6).
// Ya viene ordenada del backend; el front solo arma el indentado con `depth`/`parentCommentId`.
export function listComments(postId: number): Promise<CommentResponse[]> {
  return api.get<CommentResponse[]>(`/posts/${postId}/comments`)
}

// POST /posts/{postId}/comments → 201. `parentCommentId` opcional (respuesta a otro comentario;
// debe pertenecer al mismo post y estar ACTIVE, si no 400). Solo en posts ACTIVE.
export function createComment(
  postId: number,
  content: string,
  parentCommentId?: number,
): Promise<CommentResponse> {
  return api.post<CommentResponse>(`/posts/${postId}/comments`, { content, parentCommentId })
}

// PATCH /comments/{id} { content } → edición. Solo autor, solo si está ACTIVE.
export function updateComment(commentId: number, content: string): Promise<CommentResponse> {
  return api.patch<CommentResponse>(`/comments/${commentId}`, { content })
}

// PATCH /comments/{id}/status { status } → transición de estado.
// Autor: ACTIVE → DELETED_BY_USER. Moderador: HIDDEN / ACTIVE / DELETED_BY_MOD.
export function changeCommentStatus(commentId: number, status: string): Promise<CommentResponse> {
  return api.patch<CommentResponse>(`/comments/${commentId}/status`, { status })
}

// POST /comments/{id}/pin → fija el comentario como principal del post. Solo el autor del post.
export function pinComment(commentId: number): Promise<CommentResponse> {
  return api.post<CommentResponse>(`/comments/${commentId}/pin`)
}

// DELETE /comments/{id}/pin → desfija el comentario (204). Solo el autor del post. Idempotente.
export function unpinComment(commentId: number): Promise<void> {
  return api.delete<void>(`/comments/${commentId}/pin`)
}
