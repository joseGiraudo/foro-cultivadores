import { Link, useParams } from 'react-router-dom'
import { CommentForm } from '../components/CommentForm'
import { CommentTree } from '../components/CommentTree'
import { EditedBadge } from '../components/EditedBadge'
import { MarkdownContent } from '../components/MarkdownContent'
import { PostGallery } from '../components/PostGallery'
import { PostModerationActions } from '../components/PostModerationActions'
import { PostTypeBadge } from '../components/PostTypeBadge'
import { PostVoteControl } from '../components/PostVoteControl'
import { RoleBadge } from '../components/RoleBadge'
import { SaveButton } from '../components/SaveButton'
import { StatusBadge } from '../components/StatusBadge'
import { ErrorState, Loading } from '../components/states'
import { useAuth } from '../auth/useAuth'
import { useComments } from '../hooks/useComments'
import { useCreateComment } from '../hooks/useCommentMutations'
import { usePost } from '../hooks/usePosts'
import { formatDate } from '../lib/format'

export function PostDetailPage() {
  const { id } = useParams<{ id: string }>()
  const postId = Number(id)
  const { user, isSuspended } = useAuth()
  const createComment = useCreateComment(postId)

  const { data: post, isLoading, isError, error, refetch } = usePost(postId)
  const { data: comments, isLoading: loadingComments } = useComments(postId)

  if (isLoading) return <Loading />
  if (isError) return <ErrorState error={error} onRetry={refetch} />
  if (!post) return null

  return (
    <article>
      <Link to="/" className="text-sm font-semibold text-leaf-700 hover:underline">
        ← Volver
      </Link>

      <div className="mt-3 rounded-xl border border-sand-200 bg-cream-100 p-4 shadow-soft sm:p-6">
        <div className="flex flex-wrap items-center gap-2">
          <PostTypeBadge type={post.postType} />
          <StatusBadge status={post.status} />
          {post.categories.map((category) => (
            <span key={category.id} className="text-xs text-bark-600">
              #{category.name}
            </span>
          ))}
        </div>

        <div className="mb-2 mt-2 flex items-start justify-between gap-3">
          <h1 className="min-w-0 break-words text-2xl font-semibold text-ink sm:text-3xl">
            {post.title}
          </h1>
          <div className="flex shrink-0 items-center gap-2">
            {post.status === 'ACTIVE' && <SaveButton post={post} />}
            {user?.id === post.author.id && post.status === 'ACTIVE' && (
              <Link
                to={`/posts/${post.id}/edit`}
                className="rounded-lg border border-sand-200 px-3 py-1 text-sm font-semibold text-ink-soft transition hover:bg-leaf-100 hover:text-leaf-700"
              >
                Editar
              </Link>
            )}
          </div>
        </div>

        <div className="mb-4 flex flex-wrap items-center gap-3 text-sm text-ink-faint">
          <Link
            to={`/users/${post.author.id}`}
            className="flex items-center gap-1 hover:underline"
          >
            {post.author.username}
            <RoleBadge role={post.author.role} />
          </Link>
          <PostVoteControl post={post} />
          <span>💬 {post.commentCount}</span>
          <span>{formatDate(post.createdAt)}</span>
          <EditedBadge editedAt={post.editedAt} />
        </div>

        <div className="mb-4">
          <PostModerationActions post={post} />
        </div>

        {/* En consultas las imágenes se muestran en galería; en artículos/guías van embebidas en el Markdown. */}
        {post.postType === 'QUESTION' && <PostGallery images={post.images} />}

        <MarkdownContent content={post.content} />
      </div>

      <section className="mt-8">
        <h2 className="mb-3 text-xl font-semibold text-ink">
          Comentarios ({post.commentCount})
        </h2>

        {post.status === 'ACTIVE' && (
          <div className="mb-4">
            {!user ? (
              <p className="text-sm text-ink-soft">
                <Link to="/login" className="font-semibold text-leaf-700 hover:underline">
                  Ingresá
                </Link>{' '}
                para comentar.
              </p>
            ) : isSuspended ? (
              <p className="text-sm text-ink-soft">
                No podés comentar: tu cuenta está suspendida.
              </p>
            ) : (
              <CommentForm
                submitLabel="Comentar"
                clearOnSuccess
                onSubmit={(content) => createComment.mutateAsync({ content }).then(() => undefined)}
              />
            )}
          </div>
        )}

        {loadingComments ? (
          <Loading />
        ) : (
          <CommentTree
            comments={comments ?? []}
            postId={postId}
            postAuthorId={post.author.id}
            canComment={!!user && !isSuspended && post.status === 'ACTIVE'}
          />
        )}
      </section>
    </article>
  )
}
