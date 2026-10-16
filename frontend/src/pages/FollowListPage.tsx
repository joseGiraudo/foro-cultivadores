import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { Avatar } from '../components/Avatar'
import { Pagination } from '../components/Pagination'
import { RoleBadge } from '../components/RoleBadge'
import { EmptyState, ErrorState, Loading } from '../components/states'
import { useFollowers, useFollowing } from '../hooks/useUsers'

// Listado paginado de seguidores o seguidos. `mode` decide qué endpoint usar.
export function FollowListPage({ mode }: { mode: 'followers' | 'following' }) {
  const { id } = useParams<{ id: string }>()
  const userId = Number(id)
  const [page, setPage] = useState(0)

  const followers = useFollowers(userId, page)
  const following = useFollowing(userId, page)
  const query = mode === 'followers' ? followers : following

  const title = mode === 'followers' ? 'Seguidores' : 'Siguiendo'
  const emptyMessage =
    mode === 'followers' ? 'Todavía no tiene seguidores.' : 'Todavía no sigue a nadie.'

  return (
    <div className="mx-auto max-w-2xl">
      <Link to={`/users/${userId}`} className="text-sm font-semibold text-leaf-700 hover:underline">
        ← Volver al perfil
      </Link>
      <h1 className="mb-4 mt-2 text-2xl font-semibold text-ink">{title}</h1>

      {query.isLoading && <Loading />}
      {query.isError && <ErrorState error={query.error} onRetry={query.refetch} />}

      {query.data && (
        <>
          {query.data.content.length === 0 ? (
            <EmptyState message={emptyMessage} />
          ) : (
            <ul className="space-y-2">
              {query.data.content.map((person) => (
                <li key={person.id}>
                  <Link
                    to={`/users/${person.id}`}
                    className="flex items-center gap-3 rounded-xl border border-sand-200 bg-cream-100 p-3 shadow-soft transition hover:border-leaf-300 hover:shadow-raised"
                  >
                    <Avatar name={person.username} size="sm" />
                    <div className="flex min-w-0 flex-1 items-center gap-2">
                      <span className="font-semibold text-ink">{person.username}</span>
                      <RoleBadge role={person.role} />
                      {person.bio && (
                        <span className="truncate text-sm text-ink-faint">{person.bio}</span>
                      )}
                    </div>
                  </Link>
                </li>
              ))}
            </ul>
          )}
          <Pagination
            page={query.data.number}
            totalPages={query.data.totalPages}
            onChange={setPage}
          />
        </>
      )}
    </div>
  )
}
