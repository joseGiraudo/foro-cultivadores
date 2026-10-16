import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { ApiError } from '../api/client'
import { Avatar } from '../components/Avatar'
import { MarkdownEditor } from '../components/MarkdownEditor'
import { RoleBadge } from '../components/RoleBadge'
import { useAuth } from '../auth/useAuth'
import { useUpdateMe } from '../hooks/useUsers'
import { formatDate } from '../lib/format'
import { isAtLeast } from '../lib/roles'

export function MePage() {
  const { user } = useAuth()
  const updateMe = useUpdateMe()

  // La ruta está bajo RequireAuth, así que user no debería ser null; guard defensivo.
  const [bio, setBio] = useState(user?.bio ?? '')
  const [error, setError] = useState<string | null>(null)
  const [saved, setSaved] = useState(false)

  if (!user) return null

  const isPro = isAtLeast(user.role, 'CONTRIBUTOR')

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setSaved(false)
    try {
      await updateMe.mutateAsync(bio)
      setSaved(true)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo guardar la bio.')
    }
  }

  return (
    <div className="mx-auto max-w-2xl">
      <div className="mb-6 flex items-center gap-4 rounded-xl border border-sand-200 bg-cream-100 p-4 shadow-soft sm:p-5">
        <Avatar name={user.username} size="lg" />
        <div>
          <div className="flex flex-wrap items-center gap-2">
            <h1 className="text-2xl font-semibold text-ink">{user.username}</h1>
            <RoleBadge role={user.role} />
          </div>
          {isPro && (
            <p className="mt-0.5 text-sm font-semibold text-leaf-700">🌿 Cultivador profesional</p>
          )}
          <p className="mt-1 text-sm text-ink-faint">{user.email}</p>
        </div>
      </div>

      <div className="mb-6 flex flex-wrap gap-2">
        <span className="rounded-full bg-cream-50 px-3 py-1 text-sm text-ink-soft ring-1 ring-sand-200">
          <strong className="text-ink">{user.followerCount}</strong> seguidores
        </span>
        <span className="rounded-full bg-cream-50 px-3 py-1 text-sm text-ink-soft ring-1 ring-sand-200">
          <strong className="text-ink">{user.followingCount}</strong> siguiendo
        </span>
        <span className="rounded-full bg-cream-50 px-3 py-1 text-sm text-ink-soft ring-1 ring-sand-200">
          Desde {formatDate(user.createdAt)}
        </span>
      </div>

      <form onSubmit={handleSubmit} className="space-y-3">
        <div>
          <div className="mb-1 flex items-center justify-between">
            <span className="text-sm font-semibold text-ink">Bio</span>
            <span className="text-xs text-ink-faint">{bio.length}/500</span>
          </div>
          <MarkdownEditor
            value={bio}
            onChange={(value) => setBio(value.slice(0, 500))}
            placeholder="Contá sobre vos o tu growshop. Podés usar Markdown: **negritas**, listas y [links](https://…)."
            rows={6}
          />
          <p className="mt-1 text-xs text-ink-faint">
            Admite Markdown (negritas, listas, links). Así se verá en tu perfil público.
          </p>
        </div>

        {error && <p className="text-sm text-red-700">{error}</p>}
        {saved && <p className="text-sm text-leaf-700">Bio actualizada.</p>}

        <div className="flex gap-3">
          <button
            type="submit"
            disabled={updateMe.isPending}
            className="rounded-lg bg-leaf-600 px-4 py-2 font-semibold text-cream-50 shadow-soft transition hover:bg-leaf-700 disabled:opacity-50"
          >
            {updateMe.isPending ? 'Guardando…' : 'Guardar'}
          </button>
          <Link
            to={`/users/${user.id}`}
            className="rounded-lg border border-sand-200 px-4 py-2 font-semibold text-ink-soft transition hover:bg-leaf-100 hover:text-leaf-700"
          >
            Ver mi perfil público
          </Link>
        </div>
      </form>
    </div>
  )
}
