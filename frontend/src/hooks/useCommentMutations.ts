import { useMutation, useQueryClient } from '@tanstack/react-query'
import {
  changeCommentStatus,
  createComment,
  pinComment,
  unpinComment,
  updateComment,
} from '../api/comments'

// Al crear/editar comentarios se invalida la lista y también el post
// (el detalle muestra commentCount, que cambia al comentar).
function useCommentInvalidation(postId: number) {
  const qc = useQueryClient()
  return () => {
    qc.invalidateQueries({ queryKey: ['comments', postId] })
    qc.invalidateQueries({ queryKey: ['post', postId] })
  }
}

export function useCreateComment(postId: number) {
  const invalidate = useCommentInvalidation(postId)
  return useMutation({
    mutationFn: ({ content, parentCommentId }: { content: string; parentCommentId?: number }) =>
      createComment(postId, content, parentCommentId),
    onSuccess: invalidate,
  })
}

export function useUpdateComment(postId: number) {
  const invalidate = useCommentInvalidation(postId)
  return useMutation({
    mutationFn: ({ commentId, content }: { commentId: number; content: string }) =>
      updateComment(commentId, content),
    onSuccess: invalidate,
  })
}

// Cambio de estado de comentario: borrado por el autor (DELETED_BY_USER) o moderación
// (HIDDEN / ACTIVE / DELETED_BY_MOD).
export function useChangeCommentStatus(postId: number) {
  const invalidate = useCommentInvalidation(postId)
  return useMutation({
    mutationFn: ({ commentId, status }: { commentId: number; status: string }) =>
      changeCommentStatus(commentId, status),
    onSuccess: invalidate,
  })
}

// Fijar/desfijar un comentario como principal del post. Solo el autor del post.
export function usePinComment(postId: number) {
  const invalidate = useCommentInvalidation(postId)
  return useMutation({
    mutationFn: async ({ commentId, next }: { commentId: number; next: boolean }) => {
      // Descartamos el body (el pin devuelve CommentResponse, el unpin void);
      // nos apoyamos en la invalidación de onSuccess para reconciliar.
      if (next) await pinComment(commentId)
      else await unpinComment(commentId)
    },
    onSuccess: invalidate,
  })
}

