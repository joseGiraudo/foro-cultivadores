import { useState } from 'react'
import type { PostType } from '../api/types'
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

export function HomePage() {
  const [categoryId, setCategoryId] = useState<number | undefined>()
  const [postType, setPostType] = useState<PostType | undefined>()
  const [page, setPage] = useState(0)

  const { data: categories } = useCategories()
  const { data, isLoading, isError, error, refetch } = usePosts({ categoryId, postType, page })

  // Al cambiar un filtro, volver a la primera página.
  function updateCategory(value: number | undefined) {
    setCategoryId(value)
    setPage(0)
  }
  function updateType(value: PostType | undefined) {
    setPostType(value)
    setPage(0)
  }

  const selectClass = 'rounded-md border border-gray-300 px-3 py-2 text-sm'

  return (
    <div>
      <h1 className="mb-4 text-2xl font-bold text-gray-900">Publicaciones</h1>

      <div className="mb-4 flex flex-wrap gap-3">
        <select
          className={selectClass}
          value={postType ?? ''}
          onChange={(e) => updateType((e.target.value || undefined) as PostType | undefined)}
        >
          <option value="">Todos los tipos</option>
          {TYPE_OPTIONS.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>

        <select
          className={selectClass}
          value={categoryId ?? ''}
          onChange={(e) => updateCategory(e.target.value ? Number(e.target.value) : undefined)}
        >
          <option value="">Todas las categorías</option>
          {categories?.map((category) => (
            <option key={category.id} value={category.id}>
              {category.name}
            </option>
          ))}
        </select>
      </div>

      {isLoading && <Loading />}
      {isError && <ErrorState error={error} onRetry={refetch} />}

      {data && (
        <>
          {data.content.length === 0 ? (
            <EmptyState message="No hay publicaciones que coincidan con el filtro." />
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
