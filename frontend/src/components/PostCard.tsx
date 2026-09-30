import { Link } from 'react-router-dom'
import type { PostSummaryResponse } from '../api/types'
import { formatDate, resolveImageUrl } from '../lib/format'
import { EditedBadge } from './EditedBadge'
import { PostTypeBadge } from './PostTypeBadge'
import { RoleBadge } from './RoleBadge'
import { StatusBadge } from './StatusBadge'

export function PostCard({ post }: { post: PostSummaryResponse }) {
  return (
    <Link
      to={`/posts/${post.id}`}
      className="flex gap-4 rounded-md border border-gray-200 bg-white p-4 transition hover:border-green-400 hover:shadow-sm"
    >
      {post.thumbnailUrl && (
        <img
          src={resolveImageUrl(post.thumbnailUrl)}
          alt=""
          loading="lazy"
          className="h-20 w-20 flex-shrink-0 rounded-md object-cover"
        />
      )}
      <div className="min-w-0 flex-1">
        <div className="mb-1 flex flex-wrap items-center gap-2">
          <PostTypeBadge type={post.postType} />
          <StatusBadge status={post.status} />
          {post.categories.map((category) => (
            <span key={category.id} className="text-xs text-gray-500">
              #{category.name}
            </span>
          ))}
        </div>
        <h2 className="truncate text-lg font-semibold text-gray-900">{post.title}</h2>
        <p className="line-clamp-2 text-sm text-gray-600">{post.excerpt}</p>
        <div className="mt-2 flex flex-wrap items-center gap-3 text-xs text-gray-500">
          <span className="flex items-center gap-1">
            {post.author.username}
            <RoleBadge role={post.author.role} />
          </span>
          <span>▲ {post.score}</span>
          <span>💬 {post.commentCount}</span>
          <span>{formatDate(post.createdAt)}</span>
          <EditedBadge editedAt={post.editedAt} />
        </div>
      </div>
    </Link>
  )
}
