import { Link, useNavigate } from 'react-router-dom'
import type { PostSummaryResponse } from '../api/types'
import { formatDate, resolveImageUrl } from '../lib/format'
import { Avatar } from './Avatar'
import { EditedBadge } from './EditedBadge'
import { PostTypeBadge } from './PostTypeBadge'
import { RoleBadge } from './RoleBadge'
import { StatusBadge } from './StatusBadge'

export function PostCard({ post }: { post: PostSummaryResponse }) {
  const navigate = useNavigate()

  // La tarjeta entera es un <Link> al post, así que el autor no puede ser otro <a> anidado:
  // interceptamos el click (y Enter) para navegar al perfil sin disparar el link del post.
  function goToAuthor(e: React.SyntheticEvent) {
    e.preventDefault()
    e.stopPropagation()
    navigate(`/users/${post.author.username}`)
  }

  return (
    <Link
      to={`/posts/${post.id}`}
      className="flex gap-4 rounded-xl border border-sand-200 bg-cream-100 p-4 shadow-soft transition hover:-translate-y-0.5 hover:border-leaf-300 hover:shadow-raised"
    >
      {post.thumbnailUrl && (
        <img
          src={resolveImageUrl(post.thumbnailUrl)}
          alt=""
          loading="lazy"
          className="h-20 w-20 flex-shrink-0 rounded-lg object-cover"
        />
      )}
      <div className="min-w-0 flex-1">
        <div className="mb-1 flex flex-wrap items-center gap-2">
          <PostTypeBadge type={post.postType} />
          <StatusBadge status={post.status} />
          {post.categories.map((category) => (
            <span key={category.id} className="text-xs text-bark-600">
              #{category.name}
            </span>
          ))}
        </div>
        <h2 className="truncate text-lg font-semibold text-ink">{post.title}</h2>
        <p className="line-clamp-2 text-sm text-ink-soft">{post.excerpt}</p>
        <div className="mt-2 flex flex-wrap items-center gap-3 text-xs text-ink-faint">
          <span
            role="link"
            tabIndex={0}
            onClick={goToAuthor}
            onKeyDown={(e) => {
              if (e.key === 'Enter') goToAuthor(e)
            }}
            className="flex cursor-pointer items-center gap-1 transition hover:text-leaf-700"
          >
            <Avatar name={post.author.username} src={post.author.avatarUrl} size="xs" />
            {post.author.username}
            <RoleBadge role={post.author.role} />
          </span>
          <span className="text-leaf-700">▲ {post.score}</span>
          <span>💬 {post.commentCount}</span>
          <span>🔖 {post.saveCount}</span>
          <span>{formatDate(post.createdAt)}</span>
          <EditedBadge editedAt={post.editedAt} />
        </div>
      </div>
    </Link>
  )
}
