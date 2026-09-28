import { useState } from 'react'
import { Navigate } from 'react-router-dom'
import type { ContentStatus } from '../api/types'
import { Pagination } from '../components/Pagination'
import { PostCard } from '../components/PostCard'
import { EmptyState, ErrorState, Loading } from '../components/states'
import { useAuth } from '../auth/useAuth'
import { usePosts } from '../hooks/usePosts'
import { isAtLeast } from '../lib/roles'

const STATUS_OPTIONS: { value: '' | ContentStatus; label: string }[] = [
  { value: '', label: 'Todos los estados' },
  { value: 'ACTIVE', label: 'Activos' },
  { value: 'HIDDEN', label: 'Ocultos' },
  { value: 'DELETED_BY_USER', label: 'Eliminados por autor' },
  { value: 'DELETED_BY_MOD', label: 'Eliminados por moderación' },
]

export function ModerationPage() {
  const { user, initializing } = useAuth()
  const [status, setStatus] = useState<'' | ContentStatus>('HIDDEN')
  const [page, setPage] = useState(0)

  // Filtrar por estado usa el permiso de moderador del backend; el status vacío = ver todos.
  const { data, isLoading, isError, error, refetch } = usePosts({
    status: status || undefined,
    page,
  })

  if (initializing) return <Loading />
  // Gating cosmético; el backend igual rechaza el ?status= a no-moderadores.
  if (!isAtLeast(user?.role, 'MODERATOR')) return <Navigate to="/" replace />

  return (
    <div>
      <h1 className="mb-4 text-2xl font-bold text-gray-900">Moderación</h1>
      <p className="mb-4 text-sm text-gray-600">
        Revisá publicaciones por estado. Entrá al detalle para ocultar, restaurar o eliminar
        (también podés moderar los comentarios desde ahí).
      </p>

      <select
        className="mb-4 rounded-md border border-gray-300 px-3 py-2 text-sm"
        value={status}
        onChange={(e) => {
          setStatus(e.target.value as '' | ContentStatus)
          setPage(0)
        }}
      >
        {STATUS_OPTIONS.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>

      {isLoading && <Loading />}
      {isError && <ErrorState error={error} onRetry={refetch} />}

      {data && (
        <>
          {data.content.length === 0 ? (
            <EmptyState message="No hay publicaciones en este estado." />
          ) : (
            <div className="space-y-3">
              {data.content.map((post) => (
                <PostCard key={post.id} post={post} />
              ))}
            </div>
          )}
          <Pagination page={data.number} totalPages={data.totalPages} onChange={setPage} />
        </>
      )}
    </div>
  )
}
