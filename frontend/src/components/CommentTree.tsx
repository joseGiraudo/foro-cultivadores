import { useState } from 'react'
import type { CommentResponse } from '../api/types'
import { useAuth } from '../auth/useAuth'
import {
  useChangeCommentStatus,
  useCreateComment,
  usePinComment,
  useUpdateComment,
} from '../hooks/useCommentMutations'
import { useCommentVote } from '../hooks/useVote'
import { formatDate } from '../lib/format'
import { moderationTargets } from '../lib/moderation'
import { isAtLeast } from '../lib/roles'
import { CommentForm } from './CommentForm'
import { EditedBadge } from './EditedBadge'
import { StatusBadge } from './StatusBadge'
import { VoteControl } from './VoteControl'

// La lista ya viene en recorrido en profundidad; el indentado se deriva de `depth`.
// Tope de sangría para no romper el layout en hilos muy profundos.
const MAX_INDENT = 6

interface CommentItemProps {
  comment: CommentResponse
  postId: number
  // Id del autor del post: solo él puede fijar/desfijar comentarios.
  postAuthorId: number
  // Si el usuario puede comentar/responder (sesión activa, no suspendido, post ACTIVE).
  canComment: boolean
}

function CommentItem({ comment, postId, postAuthorId, canComment }: CommentItemProps) {
  const { user, isSuspended } = useAuth()
  const vote = useCommentVote(postId, comment.id)
  const createReply = useCreateComment(postId)
  const updateComment = useUpdateComment(postId)
  const changeStatus = useChangeCommentStatus(postId)
  const pin = usePinComment(postId)

  const [replying, setReplying] = useState(false)
  const [editing, setEditing] = useState(false)

  const indent = Math.min(comment.depth, MAX_INDENT)
  const hidden = comment.content === null

  const isOwn = user?.id === comment.author.id
  const disabled = !user || isOwn || isSuspended
  const voteTitle = !user
    ? 'Ingresá para votar'
    : isSuspended
      ? 'Tu cuenta está suspendida'
      : isOwn
        ? 'No podés votar tu propio comentario'
        : undefined

  const canEdit = isOwn && comment.status === 'ACTIVE' && !isSuspended
  const canDelete = isOwn && comment.status === 'ACTIVE' && !isSuspended
  // Solo el autor del post puede fijar, y solo comentarios de primer nivel activos.
  const isPostAuthor = user?.id === postAuthorId
  const canPin = isPostAuthor && comment.depth === 0 && comment.status === 'ACTIVE' && !isSuspended
  const isModerator = isAtLeast(user?.role, 'MODERATOR')
  const modTargets = isModerator ? moderationTargets(comment.status) : []

  async function handleReply(content: string) {
    await createReply.mutateAsync({ content, parentCommentId: comment.id })
    setReplying(false)
  }

  async function handleEdit(content: string) {
    await updateComment.mutateAsync({ commentId: comment.id, content })
    setEditing(false)
  }

  function handleDelete() {
    if (!window.confirm('¿Eliminar tu comentario?')) return
    changeStatus.mutate({ commentId: comment.id, status: 'DELETED_BY_USER' })
  }

  return (
    <div
      className={`border-l py-2 pl-3 ${
        comment.pinned ? 'border-green-500 bg-green-50/60' : 'border-gray-200'
      }`}
      style={{ marginLeft: `${indent * 1.25}rem` }}
    >
      {hidden ? (
        <p className="text-sm italic text-gray-400">
          {comment.status === 'HIDDEN' ? 'Comentario oculto' : 'Comentario eliminado'}
        </p>
      ) : (
        <>
          <div className="mb-1 flex items-center gap-2 text-xs text-gray-500">
            {comment.pinned && (
              <span className="rounded-full bg-green-600 px-2 py-0.5 text-xs font-medium text-white">
                📌 Fijado
              </span>
            )}
            <span className="font-medium text-gray-800">{comment.author.username}</span>
            <span>{formatDate(comment.createdAt)}</span>
            <EditedBadge editedAt={comment.editedAt} />
            <StatusBadge status={comment.status} />
          </div>

          {editing ? (
            <CommentForm
              initialValue={comment.content ?? ''}
              submitLabel="Guardar"
              onSubmit={handleEdit}
              onCancel={() => setEditing(false)}
              autoFocus
            />
          ) : (
            <>
              <p className="mb-1 whitespace-pre-wrap text-sm text-gray-800">{comment.content}</p>
              <div className="flex items-center gap-3">
                <VoteControl
                  score={comment.score}
                  myVote={comment.myVote}
                  onVote={(next) => vote.mutate(next)}
                  disabled={disabled}
                  title={voteTitle}
                  pending={vote.isPending}
                />
                {canComment && (
                  <button
                    type="button"
                    onClick={() => setReplying((value) => !value)}
                    className="text-xs font-medium text-gray-500 hover:text-gray-800"
                  >
                    Responder
                  </button>
                )}
                {canEdit && (
                  <button
                    type="button"
                    onClick={() => setEditing(true)}
                    className="text-xs font-medium text-gray-500 hover:text-gray-800"
                  >
                    Editar
                  </button>
                )}
                {canDelete && (
                  <button
                    type="button"
                    onClick={handleDelete}
                    disabled={changeStatus.isPending}
                    className="text-xs font-medium text-red-600 hover:text-red-800 disabled:opacity-50"
                  >
                    Eliminar
                  </button>
                )}
                {canPin && (
                  <button
                    type="button"
                    onClick={() => pin.mutate({ commentId: comment.id, next: !comment.pinned })}
                    disabled={pin.isPending}
                    className="text-xs font-medium text-green-700 hover:text-green-900 disabled:opacity-50"
                  >
                    {comment.pinned ? 'Desfijar' : 'Fijar'}
                  </button>
                )}
                {modTargets.map((target) => (
                  <button
                    key={target.status}
                    type="button"
                    disabled={changeStatus.isPending}
                    onClick={() => changeStatus.mutate({ commentId: comment.id, status: target.status })}
                    className="text-xs font-medium text-blue-700 hover:text-blue-900 disabled:opacity-50"
                  >
                    {target.label}
                  </button>
                ))}
              </div>
            </>
          )}

          {replying && (
            <div className="mt-2">
              <CommentForm
                submitLabel="Responder"
                placeholder="Escribí una respuesta…"
                onSubmit={handleReply}
                onCancel={() => setReplying(false)}
                autoFocus
              />
            </div>
          )}
        </>
      )}
    </div>
  )
}

interface CommentTreeProps {
  comments: CommentResponse[]
  postId: number
  postAuthorId: number
  canComment: boolean
}

export function CommentTree({ comments, postId, postAuthorId, canComment }: CommentTreeProps) {
  if (comments.length === 0) {
    return <p className="text-sm text-gray-500">Todavía no hay comentarios.</p>
  }
  return (
    <div>
      {comments.map((comment) => (
        <CommentItem
          key={comment.id}
          comment={comment}
          postId={postId}
          postAuthorId={postAuthorId}
          canComment={canComment}
        />
      ))}
    </div>
  )
}
