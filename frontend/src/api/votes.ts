import { api } from './client'
import type { VoteResponse, VoteValue } from './types'

// PUT .../vote es idempotente (crea o actualiza) y devuelve { score, myVote }.
// DELETE .../vote quita el voto (204, idempotente aunque no exista).

export function votePost(postId: number, value: VoteValue): Promise<VoteResponse> {
  return api.put<VoteResponse>(`/posts/${postId}/vote`, { value })
}

export function clearPostVote(postId: number): Promise<void> {
  return api.delete<void>(`/posts/${postId}/vote`)
}

export function voteComment(commentId: number, value: VoteValue): Promise<VoteResponse> {
  return api.put<VoteResponse>(`/comments/${commentId}/vote`, { value })
}

export function clearCommentVote(commentId: number): Promise<void> {
  return api.delete<void>(`/comments/${commentId}/vote`)
}
