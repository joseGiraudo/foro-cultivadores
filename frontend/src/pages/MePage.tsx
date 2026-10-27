import { useRef, useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ApiError } from '../api/client'
import type { UpdateMeInput } from '../api/types'
import { Avatar } from '../components/Avatar'
import { BecomeContributorSection } from '../components/BecomeContributorSection'
import { MarkdownEditor } from '../components/MarkdownEditor'
import { RoleBadge } from '../components/RoleBadge'
import { useAuth } from '../auth/useAuth'
import { useUpdateMe, useUserImageMutations } from '../hooks/useUsers'
import { formatDate, resolveImageUrl } from '../lib/format'
import { isAtLeast } from '../lib/roles'
import { useToast } from '../toast/useToast'

export function MePage() {
  const { user, deleteAccount } = useAuth()
  const updateMe = useUpdateMe()
  const toast = useToast()
  const navigate = useNavigate()

  // La ruta está bajo RequireAuth, así que user no debería ser null; guard defensivo.
  const [bio, setBio] = useState(user?.bio ?? '')
  const [website, setWebsite] = useState(user?.website ?? '')
  const [phone, setPhone] = useState(user?.phone ?? '')
  const [instagram, setInstagram] = useState(user?.instagram ?? '')
  const [location, setLocation] = useState(user?.location ?? '')
  const [error, setError] = useState<string | null>(null)

  const images = useUserImageMutations()
  const avatarInput = useRef<HTMLInputElement>(null)
  const coverInput = useRef<HTMLInputElement>(null)

  if (!user) return null

  const isPro = isAtLeast(user.role, 'CONTRIBUTOR')

  function notifyImageError(err: unknown) {
    if (err instanceof ApiError) {
      if (err.status === 413) toast.error('La imagen supera el tamaño máximo (5 MB).')
      else if (err.status === 415) toast.error('Tipo no permitido (usá JPEG, PNG o WebP).')
      else if (err.status === 403) toast.error('Tu rol no permite subir esta imagen.')
      else toast.error(err.message)
    } else {
      toast.error('No se pudo procesar la imagen.')
    }
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!user) return
    setError(null)
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
      toast.success('Perfil actualizado.')
      // Al guardar salimos del editor hacia el perfil público.
      navigate(`/users/${user.username}`)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo guardar el perfil.')
    }
  }

  async function handleDeleteAccount() {
    const ok = window.confirm(
      '¿Eliminar tu cuenta? Se anonimizan tus datos y no vas a poder volver a ingresar. ' +
        'Tus posts y comentarios quedan como "[cuenta eliminada]". No se puede deshacer.',
    )
    if (!ok) return
    try {
      await deleteAccount()
      toast.success('Tu cuenta fue eliminada.')
      navigate('/', { replace: true })
    } catch {
      toast.error('No se pudo eliminar la cuenta. Intentá de nuevo.')
    }
  }

  return (
    <div className="mx-auto max-w-2xl">
      <div className="mb-6 flex items-center gap-4 rounded-xl border border-sand-200 bg-cream-100 p-4 shadow-soft sm:p-5">
        <Avatar name={user.username} src={user.avatarUrl} size="lg" />
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

      <BecomeContributorSection />

      <section className="mb-6 space-y-4 rounded-xl border border-sand-200 bg-cream-100 p-4 shadow-soft sm:p-5">
        <h2 className="font-display text-lg font-semibold text-ink">Imágenes</h2>

        {/* Foto de perfil (cualquier usuario) */}
        <div className="flex items-center gap-4">
          <Avatar name={user.username} src={user.avatarUrl} size="md" />
          <div className="flex flex-wrap gap-2">
            <input
              ref={avatarInput}
              type="file"
              accept="image/jpeg,image/png,image/webp"
              className="hidden"
              onChange={(e) => {
                const file = e.target.files?.[0]
                if (avatarInput.current) avatarInput.current.value = ''
                if (!file) return
                images.uploadAvatar.mutate(file, {
                  onSuccess: () => toast.success('Foto de perfil actualizada.'),
                  onError: notifyImageError,
                })
              }}
            />
            <button
              type="button"
              onClick={() => avatarInput.current?.click()}
              disabled={images.uploadAvatar.isPending}
              className="btn-secondary px-3 py-1.5 text-sm"
            >
              {user.avatarUrl ? 'Cambiar foto' : 'Subir foto de perfil'}
            </button>
            {user.avatarUrl && (
              <button
                type="button"
                onClick={() => {
                  images.deleteAvatar.mutate(undefined, {
                    onSuccess: () => toast.success('Foto de perfil quitada.'),
                    onError: notifyImageError,
                  })
                }}
                disabled={images.deleteAvatar.isPending}
                className="rounded-lg px-3 py-1.5 text-sm font-semibold text-red-700 transition hover:bg-red-50 disabled:opacity-50"
              >
                Quitar
              </button>
            )}
          </div>
        </div>

        {/* Portada (solo Grows) */}
        {isPro && (
          <div>
            <p className="field-label">Portada</p>
            <div className="mb-2 h-24 overflow-hidden rounded-lg border border-sand-200 sm:h-32">
              {user.coverUrl ? (
                <img
                  src={resolveImageUrl(user.coverUrl)}
                  alt=""
                  className="h-full w-full object-cover"
                />
              ) : (
                <div className="h-full w-full bg-gradient-to-br from-leaf-300 via-leaf-500 to-bark-500" />
              )}
            </div>
            <div className="flex flex-wrap gap-2">
              <input
                ref={coverInput}
                type="file"
                accept="image/jpeg,image/png,image/webp"
                className="hidden"
                onChange={(e) => {
                  const file = e.target.files?.[0]
                  if (coverInput.current) coverInput.current.value = ''
                  if (!file) return
                  images.uploadCover.mutate(file, {
                    onSuccess: () => toast.success('Portada actualizada.'),
                    onError: notifyImageError,
                  })
                }}
              />
              <button
                type="button"
                onClick={() => coverInput.current?.click()}
                disabled={images.uploadCover.isPending}
                className="btn-secondary px-3 py-1.5 text-sm"
              >
                {user.coverUrl ? 'Cambiar portada' : 'Subir portada'}
              </button>
              {user.coverUrl && (
                <button
                  type="button"
                  onClick={() => {
                    images.deleteCover.mutate(undefined, {
                      onSuccess: () => toast.success('Portada quitada.'),
                      onError: notifyImageError,
                    })
                  }}
                  disabled={images.deleteCover.isPending}
                  className="rounded-lg px-3 py-1.5 text-sm font-semibold text-red-700 transition hover:bg-red-50 disabled:opacity-50"
                >
                  Quitar
                </button>
              )}
            </div>
          </div>
        )}

        <p className="text-xs text-ink-faint">JPEG, PNG o WebP · hasta 5 MB.</p>
      </section>

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

      <section className="mt-10 rounded-xl border border-red-200 bg-red-50/40 p-4">
        <h2 className="text-lg font-semibold text-ink">Eliminar mi cuenta</h2>
        <p className="mt-1 text-sm text-ink-soft">
          Se anonimizan tus datos personales (email, bio, imágenes) y no vas a poder volver a
          ingresar. Tus publicaciones y comentarios quedan como “[cuenta eliminada]”. Esta acción no
          se puede deshacer.
        </p>
        <button
          type="button"
          onClick={handleDeleteAccount}
          className="mt-3 rounded-lg border border-red-300 px-4 py-2 text-sm font-semibold text-red-700 transition hover:bg-red-100"
        >
          Eliminar mi cuenta
        </button>
      </section>
    </div>
  )
}
