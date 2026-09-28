import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { FollowButton } from '../components/FollowButton'
import { Pagination } from '../components/Pagination'
import { PostCard } from '../components/PostCard'
import { RoleBadge } from '../components/RoleBadge'
import { EmptyState, ErrorState, Loading } from '../components/states'
import { useAuth } from '../auth/useAuth'
import { usePosts } from '../hooks/usePosts'
import { useUser } from '../hooks/useUsers'
import { formatDate } from '../lib/format'

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

  return (
    <div>
      <header className="mb-6 rounded-md border border-gray-200 bg-white p-5">
        <div className="flex items-start justify-between gap-4">
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-2xl font-bold text-gray-900">{profile.username}</h1>
              <RoleBadge role={profile.role} />
            </div>
            {profile.bio && <p className="mt-2 text-gray-700">{profile.bio}</p>}
            <div className="mt-3 flex gap-4 text-sm text-gray-600">
              <Link to={`/users/${profile.id}/followers`} className="hover:underline">
                <strong className="text-gray-900">{profile.followerCount}</strong> seguidores
              </Link>
              <Link to={`/users/${profile.id}/following`} className="hover:underline">
                <strong className="text-gray-900">{profile.followingCount}</strong> siguiendo
              </Link>
              <span>Desde {formatDate(profile.createdAt)}</span>
            </div>
          </div>
          {isMe ? (
            <Link
              to="/me"
              className="shrink-0 rounded-md border border-gray-300 px-4 py-1.5 text-sm font-medium text-gray-700 hover:bg-gray-100"
            >
              Editar perfil
            </Link>
          ) : (
            <FollowButton profile={profile} />
          )}
        </div>
      </header>

      <h2 className="mb-3 text-lg font-semibold text-gray-900">Publicaciones</h2>
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
