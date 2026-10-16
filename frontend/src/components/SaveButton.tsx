import { ApiError } from '../api/client'
import type { PostResponse } from '../api/types'
import { useAuth } from '../auth/useAuth'
import { useSavePost } from '../hooks/usePostMutations'
import { useState } from 'react'

// Botón guardar/quitar de guardados de un post. Solo visible con sesión activa.
export function SaveButton({ post }: { post: PostResponse }) {
  const { user, isSuspended } = useAuth()
  const save = useSavePost(post.id)
  const [error, setError] = useState<string | null>(null)

  if (!user) return null

  const saved = post.saved

  function handleClick() {
    setError(null)
    save.mutate(!saved, {
      onError: (err) => {
        setError(err instanceof ApiError ? err.message : 'No se pudo completar la acción.')
      },
    })
  }

  return (
    <div className="flex flex-col gap-1">
      <button
        type="button"
        onClick={handleClick}
        disabled={save.isPending || isSuspended}
        title={isSuspended ? 'Tu cuenta está suspendida' : undefined}
        className={`flex items-center gap-1 rounded-lg px-3 py-1.5 text-sm font-semibold transition disabled:opacity-50 ${
          saved
            ? 'border border-leaf-600 text-leaf-700 hover:bg-leaf-100'
            : 'border border-sand-200 text-ink-soft hover:bg-leaf-100 hover:text-leaf-700'
        }`}
      >
        {saved ? '★ Guardado' : '☆ Guardar'}
      </button>
      {error && <p className="text-xs text-red-700">{error}</p>}
    </div>
  )
}
