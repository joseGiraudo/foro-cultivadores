import { useState } from 'react'
import { Pagination } from '../components/Pagination'
import { PostCard } from '../components/PostCard'
import { EmptyState, ErrorState, Loading } from '../components/states'
import { useSavedPosts } from '../hooks/usePosts'

export function SavedPostsPage() {
  const [page, setPage] = useState(0)
  const { data, isLoading, isError, error, refetch } = useSavedPosts(page)

  return (
    <div>
      <h1 className="mb-4 text-2xl font-bold text-gray-900">Guardados</h1>

      {isLoading && <Loading />}
      {isError && <ErrorState error={error} onRetry={refetch} />}

      {data && (
        <>
          {data.content.length === 0 ? (
            <EmptyState message="Todavía no guardaste ninguna publicación." />
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
