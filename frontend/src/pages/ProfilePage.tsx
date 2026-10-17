import { useEffect, useState } from 'react'
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
import { useUserByUsername } from '../hooks/useUsers'
import { formatDate, resolveImageUrl } from '../lib/format'
import { isAtLeast } from '../lib/roles'

// Asegura que la URL tenga protocolo (los usuarios suelen escribir "tugrow.com").
function toHttpUrl(url: string): string {
  return /^https?:\/\//i.test(url) ? url : `https://${url}`
}

export function ProfilePage() {
  const { username } = useParams<{ username: string }>()
  const { user: me } = useAuth()

  const { data: profile, isLoading, isError, error, refetch } = useUserByUsername(username ?? '')
  const [page, setPage] = useState(0)
  const [zoomed, setZoomed] = useState(false)
  // Los posts del autor se filtran por id; esperamos a tener el perfil cargado.
  const { data: posts } = usePosts({ authorId: profile?.id, page }, { enabled: !!profile })

  // Cerrar el visor del avatar con Escape y bloquear el scroll del fondo mientras está abierto.
  useEffect(() => {
    if (!zoomed) return
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setZoomed(false)
    }
    document.addEventListener('keydown', onKey)
    const prevOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => {
      document.removeEventListener('keydown', onKey)
      document.body.style.overflow = prevOverflow
    }
  }, [zoomed])

  if (isLoading) return <Loading />
  if (isError) return <ErrorState error={error} onRetry={refetch} />
  if (!profile) return null

  const isMe = me?.id === profile.id
  // CONTRIBUTOR+ = growers/growshops profesionales: les damos un realce para que destaquen.
  const isPro = isAtLeast(profile.role, 'CONTRIBUTOR')

  const chipClass = 'rounded-full bg-cream-50 px-3 py-1 text-sm text-ink-soft ring-1 ring-sand-200'

  return (
    <div>
      {zoomed && profile.avatarUrl && (
        <div
          role="dialog"
          aria-modal="true"
          aria-label="Foto de perfil"
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 p-4"
          onClick={() => setZoomed(false)}
        >
          <button
            type="button"
            onClick={() => setZoomed(false)}
            aria-label="Cerrar"
            className="absolute right-4 top-4 flex h-10 w-10 items-center justify-center rounded-full bg-white/10 text-2xl leading-none text-white hover:bg-white/20"
          >
            ×
          </button>
          <img
            src={resolveImageUrl(profile.avatarUrl)}
            alt={profile.username}
            className="max-h-[85vh] max-w-full rounded-lg object-contain"
            onClick={(e) => e.stopPropagation()}
          />
        </div>
      )}
      <header
        className={`mb-6 overflow-hidden rounded-xl border bg-cream-100 shadow-soft ${
          isPro ? 'border-leaf-300 ring-1 ring-leaf-200' : 'border-sand-200'
        }`}
      >
        {/* Portada: foto del usuario si tiene, si no un degradado decorativo. */}
        {profile.coverUrl ? (
          <img
            src={resolveImageUrl(profile.coverUrl)}
            alt=""
            className="h-24 w-full object-cover sm:h-36"
          />
        ) : (
          <div className="h-24 bg-gradient-to-br from-leaf-300 via-leaf-500 to-bark-500 sm:h-32" />
        )}

        <div className="px-4 pb-5 sm:px-6">
          <div className="-mt-10 flex flex-col gap-4 sm:-mt-12 sm:flex-row sm:items-end sm:justify-between">
            <div className="flex items-end gap-4">
              {profile.avatarUrl ? (
                <button
                  type="button"
                  onClick={() => setZoomed(true)}
                  aria-label="Ampliar foto de perfil"
                  className="rounded-full focus:outline-none focus:ring-2 focus:ring-leaf-500"
                >
                  <Avatar
                    name={profile.username}
                    src={profile.avatarUrl}
                    size="lg"
                    className="cursor-pointer ring-4 ring-cream-100 transition hover:opacity-90"
                  />
                </button>
              ) : (
                <Avatar
                  name={profile.username}
                  src={profile.avatarUrl}
                  size="lg"
                  className="ring-4 ring-cream-100"
                />
              )}
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

          {(profile.location || profile.website || profile.phone || profile.instagram) && (
            <div className="mt-4 flex flex-wrap gap-2">
              {profile.location && <span className={chipClass}>📍 {profile.location}</span>}
              {profile.website && (
                <a
                  href={toHttpUrl(profile.website)}
                  target="_blank"
                  rel="noopener noreferrer"
                  className={`${chipClass} hover:ring-leaf-300`}
                >
                  🌐 Sitio web
                </a>
              )}
              {profile.phone && (
                <a
                  href={`https://wa.me/${profile.phone.replace(/[^0-9]/g, '')}`}
                  target="_blank"
                  rel="noopener noreferrer"
                  className={`${chipClass} hover:ring-leaf-300`}
                >
                  💬 WhatsApp
                </a>
              )}
              {profile.instagram && (
                <a
                  href={`https://instagram.com/${profile.instagram.replace(/^@/, '')}`}
                  target="_blank"
                  rel="noopener noreferrer"
                  className={`${chipClass} hover:ring-leaf-300`}
                >
                  📸 {profile.instagram.startsWith('@') ? profile.instagram : `@${profile.instagram}`}
                </a>
              )}
            </div>
          )}

          <div className="mt-4 flex flex-wrap gap-2">
            <Link
              to={`/users/${profile.username}/followers`}
              className={`${chipClass} hover:ring-leaf-300`}
            >
              <strong className="text-ink">{profile.followerCount}</strong> seguidores
            </Link>
            <Link
              to={`/users/${profile.username}/following`}
              className={`${chipClass} hover:ring-leaf-300`}
            >
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
