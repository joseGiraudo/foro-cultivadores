import type { PostResponse } from '../api/types'
import { useAuth } from '../auth/useAuth'
import { useChangePostStatus } from '../hooks/usePostMutations'
import { isAtLeast } from '../lib/roles'
import { moderationTargets } from '../lib/moderation'

// Acciones de moderación sobre un post (MODERATOR+). La autorización real la hace el backend.
export function PostModerationActions({ post }: { post: PostResponse }) {
  const { user } = useAuth()
  const changeStatus = useChangePostStatus(post.id)

  if (!isAtLeast(user?.role, 'MODERATOR')) return null

  const targets = moderationTargets(post.status)
  if (targets.length === 0) return null

  return (
    <div className="flex flex-wrap items-center gap-2 rounded-md border border-blue-200 bg-blue-50 p-2">
      <span className="text-xs font-medium text-blue-800">Moderación:</span>
      {targets.map((target) => (
        <button
          key={target.status}
          type="button"
          disabled={changeStatus.isPending}
          onClick={() => changeStatus.mutate(target.status)}
          className="rounded border border-blue-300 bg-white px-2 py-1 text-xs font-medium text-blue-800 hover:bg-blue-100 disabled:opacity-50"
        >
          {target.label}
        </button>
      ))}
    </div>
  )
}
