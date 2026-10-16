import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { ApiError } from '../api/client'
import type { UpdateMeInput } from '../api/types'
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
  const [website, setWebsite] = useState(user?.website ?? '')
  const [phone, setPhone] = useState(user?.phone ?? '')
  const [instagram, setInstagram] = useState(user?.instagram ?? '')
  const [location, setLocation] = useState(user?.location ?? '')
  const [error, setError] = useState<string | null>(null)
  const [saved, setSaved] = useState(false)

  if (!user) return null

  const isPro = isAtLeast(user.role, 'CONTRIBUTOR')

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setSaved(false)
    const input: UpdateMeInput = { bio }
    // Solo mandamos datos de comercio si el rol los permite (el backend igual valida con 403).
    if (isPro) {
      input.website = website
      input.phone = phone
      input.instagram = instagram
      input.location = location
    }
    try {
      await updateMe.mutateAsync(input)
      setSaved(true)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo guardar el perfil.')
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

      <form onSubmit={handleSubmit} className="space-y-5">
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

        {isPro && (
          <fieldset className="space-y-3 rounded-xl border border-sand-200 bg-cream-100 p-4 shadow-soft">
            <legend className="px-1 font-display text-sm font-semibold text-leaf-700">
              🏪 Datos de comercio
            </legend>
            <p className="text-xs text-ink-faint">
              Opcionales. Se muestran en tu perfil público para que te puedan contactar.
            </p>
            <label className="block">
              <span className="field-label">Sitio web</span>
              <input
                type="url"
                value={website}
                onChange={(e) => setWebsite(e.target.value)}
                maxLength={255}
                placeholder="https://tugrowshop.com"
                className="input"
              />
            </label>
            <label className="block">
              <span className="field-label">WhatsApp / Teléfono</span>
              <input
                type="tel"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                maxLength={30}
                placeholder="+54 9 11 1234-5678"
                className="input"
              />
            </label>
            <label className="block">
              <span className="field-label">Instagram</span>
              <input
                type="text"
                value={instagram}
                onChange={(e) => setInstagram(e.target.value)}
                maxLength={50}
                placeholder="@tugrowshop"
                className="input"
              />
            </label>
            <label className="block">
              <span className="field-label">Ubicación</span>
              <input
                type="text"
                value={location}
                onChange={(e) => setLocation(e.target.value)}
                maxLength={100}
                placeholder="Ciudad, provincia"
                className="input"
              />
            </label>
          </fieldset>
        )}

        {error && <p className="text-sm text-red-700">{error}</p>}
        {saved && <p className="text-sm text-leaf-700">Perfil actualizado.</p>}

        <div className="flex gap-3">
          <button type="submit" disabled={updateMe.isPending} className="btn-primary">
            {updateMe.isPending ? 'Guardando…' : 'Guardar'}
          </button>
          <Link
            to={`/users/${user.username}`}
            className="rounded-lg border border-sand-200 px-4 py-2 font-semibold text-ink-soft transition hover:bg-leaf-100 hover:text-leaf-700"
          >
            Ver mi perfil público
          </Link>
        </div>
      </form>
    </div>
  )
}
