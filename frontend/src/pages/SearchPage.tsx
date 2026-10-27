import { useSearchParams } from 'react-router-dom'
import { Pagination } from '../components/Pagination'
import { PostCard } from '../components/PostCard'
import { EmptyState, ErrorState, Loading } from '../components/states'
import { usePosts } from '../hooks/usePosts'

/** Página de resultados de búsqueda. Lee `q` y `page` de la URL (compartible). */
export function SearchPage() {
  const [params, setParams] = useSearchParams()
  const q = (params.get('q') ?? '').trim()
  const page = Math.max(0, Number(params.get('page')) || 0)

  const { data, isLoading, isError, error, refetch } = usePosts({ q, page }, { enabled: q.length > 0 })

  function changePage(next: number) {
    const updated = new URLSearchParams(params)
    updated.set('page', String(next))
    setParams(updated)
  }

  if (!q) {
    return (
      <div>
        <h1 className="mb-4 text-3xl font-semibold text-ink">Buscar</h1>
        <EmptyState message="Escribí algo en la barra de búsqueda para encontrar publicaciones." />
      </div>
    )
  }

  return (
    <div>
      <p className="mb-4 text-sm text-ink-soft">
        {data
          ? `${data.totalElements} resultado${data.totalElements === 1 ? '' : 's'} para `
          : 'Buscando '}
        <span className="font-semibold text-ink">«{q}»</span>
      </p>

      {isLoading && <Loading />}
      {isError && <ErrorState error={error} onRetry={refetch} />}

      {data && (
        <>
          {data.content.length === 0 ? (
            <EmptyState message={`No hay resultados para «${q}».`} />
          ) : (
            <div className="space-y-3">
              {data.content.map((post) => (
                <PostCard key={post.id} post={post} />
              ))}
            </div>
          )}
          <Pagination page={data.number} totalPages={data.totalPages} onChange={changePage} />
        </>
      )}
    </div>
  )
}
