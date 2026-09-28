import { useMutation, useQueryClient } from '@tanstack/react-query'
import {
  clearCommentVote,
  clearPostVote,
  voteComment,
  votePost,
} from '../api/votes'
import type { CommentResponse, PostResponse, VoteValue } from '../api/types'

// Aporte de un voto al score. null = sin voto.
function voteScore(vote: VoteValue | null): number {
  if (vote === 'UPVOTE') return 1
  if (vote === 'DOWNVOTE') return -1
  return 0
}

// Voto de un post con actualización optimista sobre el detalle en cache.
// `next === null` quita el voto. El backend es la fuente de verdad: al asentar, se invalida.
export function usePostVote(postId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (next: VoteValue | null) => {
      if (next === null) await clearPostVote(postId)
      else await votePost(postId, next)
    },
    onMutate: async (next) => {
      await qc.cancelQueries({ queryKey: ['post', postId] })
      const prev = qc.getQueryData<PostResponse>(['post', postId])
      if (prev) {
        const upDelta = (next === 'UPVOTE' ? 1 : 0) - (prev.myVote === 'UPVOTE' ? 1 : 0)
        const downDelta = (next === 'DOWNVOTE' ? 1 : 0) - (prev.myVote === 'DOWNVOTE' ? 1 : 0)
        const upvotes = prev.upvotes + upDelta
        const downvotes = prev.downvotes + downDelta
        qc.setQueryData<PostResponse>(['post', postId], {
          ...prev,
          upvotes,
          downvotes,
          score: upvotes - downvotes,
          myVote: next,
        })
      }
      return { prev }
    },
    onError: (_error, _next, context) => {
      if (context?.prev) qc.setQueryData(['post', postId], context.prev)
    },
    onSettled: () => {
      qc.invalidateQueries({ queryKey: ['post', postId] })
      qc.invalidateQueries({ queryKey: ['posts'] })
    },
  })
}

// Voto de un comentario con actualización optimista sobre la lista plana en cache.
export function useCommentVote(postId: number, commentId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (next: VoteValue | null) => {
      if (next === null) await clearCommentVote(commentId)
      else await voteComment(commentId, next)
    },
    onMutate: async (next) => {
      await qc.cancelQueries({ queryKey: ['comments', postId] })
      const prev = qc.getQueryData<CommentResponse[]>(['comments', postId])
      if (prev) {
        qc.setQueryData<CommentResponse[]>(
          ['comments', postId],
          prev.map((comment) =>
            comment.id === commentId
              ? {
                  ...comment,
                  score: comment.score + (voteScore(next) - voteScore(comment.myVote)),
                  myVote: next,
                }
              : comment,
          ),
        )
      }
      return { prev }
    },
    onError: (_error, _next, context) => {
      if (context?.prev) qc.setQueryData(['comments', postId], context.prev)
    },
    onSettled: () => {
      qc.invalidateQueries({ queryKey: ['comments', postId] })
    },
  })
}
