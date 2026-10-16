import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { Avatar } from '../components/Avatar'
import { FollowButton } from '../components/FollowButton'
import { MarkdownContent } from '../components/MarkdownContent'
import { Pagination } from '../components/Pagination'
import { PostCard } from '../components/PostCard'
import { RoleBadge } from '../components/RoleBadge'
import { EmptyState, ErrorState, Loading } from '../components/states'
import { useAuth } from '../auth/useAuth'
import { usePosts } from '../hooks/usePosts'
import { useUser } from '../hooks/useUsers'
import { formatDate } from '../lib/format'
import { isAtLeast } from '../lib/roles'

export function ProfilePage() {
  const { id } = useParams<{ id: string }>()
  const userId = Number(id)
  const { user: me } = useAuth()

  const { data: profile, isLoading, isError, error, refetch } = useUser(userId)
  const [page, setPage] = useState(0)
  const { data: posts } = usePosts({ authorId: userId, page })

  if (isLoading) return <Loading />
  if (isError) return <ErrorState error={error} onRetry={refetch} />
  if (!profile) return null

  const isMe = me?.id === profile.id
  // CONTRIBUTOR+ = growers/growshops profesionales: les damos un realce para que destaquen.
  const isPro = isAtLeast(profile.role, 'CONTRIBUTOR')

  const chipClass = 'rounded-full bg-cream-50 px-3 py-1 text-sm text-ink-soft ring-1 ring-sand-200'

  return (
    <div>
      <header
        className={`mb-6 overflow-hidden rounded-xl border bg-cream-100 shadow-soft ${
          isPro ? 'border-leaf-300 ring-1 ring-leaf-200' : 'border-sand-200'
        }`}
      >
        {/* Portada decorativa */}
        <div className="h-24 bg-gradient-to-br from-leaf-300 via-leaf-500 to-bark-500 sm:h-32" />

        <div className="px-4 pb-5 sm:px-6">
          <div className="-mt-10 flex flex-col gap-4 sm:-mt-12 sm:flex-row sm:items-end sm:justify-between">
            <div className="flex items-end gap-4">
              <Avatar name={profile.username} size="lg" className="ring-4 ring-cream-100" />
              <div className="pb-1">
                <div className="flex flex-wrap items-center gap-2">
                  <h1 className="text-2xl font-semibold text-ink">{profile.username}</h1>
                  <RoleBadge role={profile.role} />
                </div>
                {isPro && (
                  <p className="mt-0.5 text-sm font-semibold text-leaf-700">
                    🌿 Cultivador profesional
                  </p>
                )}
              </div>
            </div>
            <div className="shrink-0">
              {isMe ? (
                <Link
                  to="/me"
                  className="inline-block rounded-lg border border-sand-200 px-4 py-1.5 text-sm font-semibold text-ink-soft transition hover:bg-leaf-100 hover:text-leaf-700"
                >
                  Editar perfil
                </Link>
              ) : (
                <FollowButton profile={profile} />
              )}
            </div>
          </div>

          {profile.bio && (
            <div className="mt-4 text-sm">
              <MarkdownContent content={profile.bio} />
            </div>
          )}

          <div className="mt-4 flex flex-wrap gap-2">
            <Link to={`/users/${profile.id}/followers`} className={`${chipClass} hover:ring-leaf-300`}>
              <strong className="text-ink">{profile.followerCount}</strong> seguidores
            </Link>
            <Link to={`/users/${profile.id}/following`} className={`${chipClass} hover:ring-leaf-300`}>
              <strong className="text-ink">{profile.followingCount}</strong> siguiendo
            </Link>
            <span className={chipClass}>Desde {formatDate(profile.createdAt)}</span>
          </div>
        </div>
      </header>

      <h2 className="mb-3 text-xl font-semibold text-ink">Publicaciones</h2>
      {posts && posts.content.length === 0 ? (
        <EmptyState message="Todavía no tiene publicaciones." />
      ) : (
        <div className="space-y-3">
          {posts?.content.map((post) => (
            <PostCard key={post.id} post={post} />
          ))}
        </div>
      )}
      {posts && <Pagination page={posts.number} totalPages={posts.totalPages} onChange={setPage} />}
    </div>
  )
}
