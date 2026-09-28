import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { ApiError } from '../api/client'
import { RoleBadge } from '../components/RoleBadge'
import { useAuth } from '../auth/useAuth'
import { useUpdateMe } from '../hooks/useUsers'
import { formatDate } from '../lib/format'

export function MePage() {
  const { user } = useAuth()
  const updateMe = useUpdateMe()

  // La ruta está bajo RequireAuth, así que user no debería ser null; guard defensivo.
  const [bio, setBio] = useState(user?.bio ?? '')
  const [error, setError] = useState<string | null>(null)
  const [saved, setSaved] = useState(false)

  if (!user) return null

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
      <div className="mb-4 flex items-center gap-2">
        <h1 className="text-2xl font-bold text-gray-900">Mi perfil</h1>
        <RoleBadge role={user.role} />
      </div>

      <dl className="mb-6 space-y-1 text-sm text-gray-700">
        <div className="flex gap-2">
          <dt className="font-medium text-gray-900">Usuario:</dt>
          <dd>{user.username}</dd>
        </div>
        <div className="flex gap-2">
          <dt className="font-medium text-gray-900">Email:</dt>
          <dd>{user.email}</dd>
        </div>
        <div className="flex gap-4">
          <span>
            <strong className="text-gray-900">{user.followerCount}</strong> seguidores
          </span>
          <span>
            <strong className="text-gray-900">{user.followingCount}</strong> siguiendo
          </span>
          <span>Desde {formatDate(user.createdAt)}</span>
        </div>
      </dl>

      <form onSubmit={handleSubmit} className="space-y-3">
        <label className="block">
          <span className="mb-1 block text-sm font-medium text-gray-700">Bio</span>
          <textarea
            value={bio}
            onChange={(e) => setBio(e.target.value)}
            rows={4}
            maxLength={500}
            placeholder="Contá algo sobre vos o tu growshop…"
            className="w-full rounded-md border border-gray-300 p-2 text-sm focus:border-green-600 focus:outline-none"
          />
          <span className="text-xs text-gray-400">{bio.length}/500</span>
        </label>

        {error && <p className="text-sm text-red-700">{error}</p>}
        {saved && <p className="text-sm text-green-700">Bio actualizada.</p>}

        <div className="flex gap-3">
          <button
            type="submit"
            disabled={updateMe.isPending}
            className="rounded-md bg-green-700 px-4 py-2 font-medium text-white hover:bg-green-800 disabled:opacity-50"
          >
            {updateMe.isPending ? 'Guardando…' : 'Guardar'}
          </button>
          <Link
            to={`/users/${user.id}`}
            className="rounded-md border border-gray-300 px-4 py-2 font-medium text-gray-700 hover:bg-gray-100"
          >
            Ver mi perfil público
          </Link>
        </div>
      </form>
    </div>
  )
}
