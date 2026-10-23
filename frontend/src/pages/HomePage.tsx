import { useSearchParams } from 'react-router-dom'
import type { PostType } from '../api/types'
import { useAuth } from '../auth/useAuth'
import { FilterChip } from '../components/FilterChip'
import { Pagination } from '../components/Pagination'
import { PostCard } from '../components/PostCard'
import { EmptyState, ErrorState, Loading } from '../components/states'
import { useCategories } from '../hooks/useCategories'
import { usePosts } from '../hooks/usePosts'

const TYPE_OPTIONS: { value: PostType; label: string }[] = [
  { value: 'QUESTION', label: 'Consultas' },
  { value: 'ARTICLE', label: 'Artículos' },
  { value: 'GUIDE', label: 'Guías' },
]

// Color de cada tipo cuando está activo (mismo lenguaje cromático que PostTypeBadge).
const TYPE_ACTIVE: Record<PostType, string> = {
  QUESTION: 'border-amber-500 bg-amber-500 text-white shadow-soft',
  ARTICLE: 'border-sky-500 bg-sky-500 text-white shadow-soft',
  GUIDE: 'border-emerald-600 bg-emerald-600 text-white shadow-soft',
}

function isPostType(value: string | null): value is PostType {
  return value === 'QUESTION' || value === 'ARTICLE' || value === 'GUIDE'
}

export function HomePage() {
  const { user } = useAuth()
  // Los filtros viven en la URL (compartibles): clickear el logo 🌱 (va a "/") los limpia solo.
  const [params, setParams] = useSearchParams()

  const rawType = params.get('type')
  const postType = isPostType(rawType) ? rawType : undefined
  const categoryIds = params.getAll('cat').map(Number).filter(Number.isInteger)
  const feed = params.get('feed') === 'following' ? 'following' : 'all'
  const page = Math.max(0, Number(params.get('page')) || 0)

  const { data: categories } = useCategories()
  // "Siguiendo" solo aplica con sesión (la pestaña se oculta deslogueado; esto lo refuerza).
  const following = feed === 'following' && !!user
  const hasActiveFilters = !!postType || categoryIds.length > 0

  const { data, isLoading, isError, error, refetch } = usePosts({
    categoryIds,
    postType,
    page,
    following,
  })

  // Escribe en la URL. Cualquier cambio de filtro vuelve a la página 0 (borra el param `page`).
  function commit(mutate: (next: URLSearchParams) => void, resetPage = true) {
    const next = new URLSearchParams(params)
    mutate(next)
    if (resetPage) next.delete('page')
    setParams(next)
  }

  function updateFeed(value: 'all' | 'following') {
    commit((p) => (value === 'following' ? p.set('feed', 'following') : p.delete('feed')))
  }
  function updateType(value: PostType | undefined) {
    commit((p) => (value ? p.set('type', value) : p.delete('type')))
  }
  function toggleCategory(id: number) {
    commit((p) => {
      const current = p.getAll('cat')
      p.delete('cat')
      const updated = current.includes(String(id))
        ? current.filter((c) => c !== String(id))
        : [...current, String(id)]
      updated.forEach((c) => p.append('cat', c))
    })
  }
  function clearCategories() {
    commit((p) => p.delete('cat'))
  }
  function clearAllFilters() {
    commit((p) => {
      p.delete('type')
      p.delete('cat')
    })
  }
  function changePage(next: number) {
    commit((p) => (next > 0 ? p.set('page', String(next)) : p.delete('page')), false)
  }

  function tabClass(active: boolean) {
    return `rounded-lg px-4 py-2 text-sm font-semibold transition ${
      active ? 'bg-leaf-600 text-cream-50 shadow-soft' : 'text-ink-soft hover:bg-cream-100 hover:text-ink'
    }`
  }

  // Ítem de filtro vertical para las sidebars de escritorio.
  function sideItem(active: boolean, activeClass?: string) {
    const base = 'w-full rounded-lg border border-transparent px-3 py-2 text-left text-sm font-medium transition'
    const inactive = 'text-ink-soft hover:bg-cream-100 hover:text-ink'
    return `${base} ${active ? (activeClass ?? 'border-leaf-600 bg-leaf-600 text-cream-50 shadow-soft') : inactive}`
  }

  function emptyMessage() {
    if (following) return 'No hay publicaciones de quienes seguís (todavía).'
    return 'No hay publicaciones que coincidan con el filtro.'
  }

  const sectionTitle = 'mb-2 px-3 text-xs font-semibold uppercase tracking-wide text-ink-faint'
  const clearButtonClass =
    'rounded-lg border border-sand-300 px-3 py-2 text-sm font-semibold text-ink-soft transition hover:bg-cream-100 hover:text-ink'

  return (
    <div className="lg:grid lg:grid-cols-[13rem_minmax(0,1fr)_14rem] lg:gap-6">
      {/* Sidebar izquierda: Categorías (solo escritorio) */}
      <aside className="hidden lg:block">
        {categories && categories.length > 0 && (
          <div className="sticky top-20">
            <h2 className={sectionTitle}>Categorías</h2>
            <nav className="flex flex-col gap-1">
              <button type="button" onClick={clearCategories} className={sideItem(categoryIds.length === 0)}>
                Todas
              </button>
              {categories.map((category) => (
                <button
                  key={category.id}
                  type="button"
                  onClick={() => toggleCategory(category.id)}
                  className={sideItem(categoryIds.includes(category.id))}
                >
                  {category.name}
                </button>
              ))}
            </nav>
          </div>
        )}
      </aside>

      {/* Centro: feed */}
      <div className="min-w-0">
        <h1 className="mb-4 text-3xl font-semibold text-ink">Publicaciones</h1>

        {user && (
          <div className="mb-4 inline-flex gap-1 rounded-xl bg-sand-200 p-1">
            <button type="button" className={tabClass(!following)} onClick={() => updateFeed('all')}>
              Todo
            </button>
            <button type="button" className={tabClass(following)} onClick={() => updateFeed('following')}>
              Siguiendo
            </button>
          </div>
        )}

        {/* Filtros inline (solo mobile/tablet; en escritorio van a las sidebars) */}
        <div className="mb-5 space-y-3 lg:hidden">
          <div className="flex flex-wrap items-center gap-2">
            <span className="mr-1 text-sm font-semibold text-ink-soft">Tipo</span>
            <FilterChip active={!postType} onClick={() => updateType(undefined)}>
              Todos
            </FilterChip>
            {TYPE_OPTIONS.map((option) => (
              <FilterChip
                key={option.value}
                active={postType === option.value}
                activeClass={TYPE_ACTIVE[option.value]}
                onClick={() => updateType(postType === option.value ? undefined : option.value)}
              >
                {option.label}
              </FilterChip>
            ))}
          </div>

          {categories && categories.length > 0 && (
            <div className="flex flex-wrap items-center gap-2">
              <span className="mr-1 text-sm font-semibold text-ink-soft">Categorías</span>
              <FilterChip active={categoryIds.length === 0} onClick={clearCategories}>
                Todas
              </FilterChip>
              {categories.map((category) => (
                <FilterChip
                  key={category.id}
                  active={categoryIds.includes(category.id)}
                  onClick={() => toggleCategory(category.id)}
                >
                  {category.name}
                </FilterChip>
              ))}
            </div>
          )}

          {hasActiveFilters && (
            <button type="button" onClick={clearAllFilters} className={clearButtonClass}>
              Limpiar filtros
            </button>
          )}
        </div>

        {isLoading && <Loading />}
        {isError && <ErrorState error={error} onRetry={refetch} />}

        {data && (
          <>
            {data.content.length === 0 ? (
              <EmptyState message={emptyMessage()} />
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

      {/* Sidebar derecha: Tipo + limpiar (solo escritorio) */}
      <aside className="hidden lg:block">
        <div className="sticky top-20 space-y-4">
          <div>
            <h2 className={sectionTitle}>Tipo</h2>
            <nav className="flex flex-col gap-1">
              <button type="button" onClick={() => updateType(undefined)} className={sideItem(!postType)}>
                Todos
              </button>
              {TYPE_OPTIONS.map((option) => (
                <button
                  key={option.value}
                  type="button"
                  onClick={() => updateType(postType === option.value ? undefined : option.value)}
                  className={sideItem(postType === option.value, TYPE_ACTIVE[option.value])}
                >
                  {option.label}
                </button>
              ))}
            </nav>
          </div>

          {hasActiveFilters && (
            <button type="button" onClick={clearAllFilters} className={`w-full ${clearButtonClass}`}>
              Limpiar filtros
            </button>
          )}
        </div>
      </aside>
    </div>
  )
}
