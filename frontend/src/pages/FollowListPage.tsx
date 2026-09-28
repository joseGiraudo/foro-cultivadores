import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
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
      <Link to={`/users/${userId}`} className="text-sm text-green-700 hover:underline">
        ← Volver al perfil
      </Link>
      <h1 className="mb-4 mt-2 text-2xl font-bold text-gray-900">{title}</h1>

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
                    className="flex items-center gap-2 rounded-md border border-gray-200 bg-white p-3 hover:border-green-400"
                  >
                    <span className="font-medium text-gray-900">{person.username}</span>
                    <RoleBadge role={person.role} />
                    {person.bio && (
                      <span className="truncate text-sm text-gray-500">{person.bio}</span>
                    )}
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
