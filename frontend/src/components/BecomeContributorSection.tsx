import { useState, type FormEvent } from 'react'
import { ApiError } from '../api/client'
import { useAuth } from '../auth/useAuth'
import {
  useCreateContributorRequest,
  useMyContributorRequests,
} from '../hooks/useContributorRequests'
import { formatDate } from '../lib/format'
import { useToast } from '../toast/useToast'

const MIN_MESSAGE = 20
const MAX_MESSAGE = 1000
const MAX_CONTACT = 200

// Sección en /me para que un usuario común pida ser promovido a Grow (CONTRIBUTOR).
// Solo se muestra a USER: los Grow+ no la necesitan. Si ya hay una petición pendiente muestra
// el estado; si no hay o la última fue rechazada, ofrece el formulario.
export function BecomeContributorSection() {
  const { user } = useAuth()
  const { data: requests } = useMyContributorRequests()
  const create = useCreateContributorRequest()
  const toast = useToast()

  const [message, setMessage] = useState('')
  const [contact, setContact] = useState('')
  const [error, setError] = useState<string | null>(null)

  // Solo para usuarios comunes (el hook ya evita el fetch para otros roles).
  if (!user || user.role !== 'USER') return null

  const latest = requests?.[0]
  const pending = latest?.status === 'PENDING'

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    if (message.trim().length < MIN_MESSAGE) {
      setError(`Contá un poco más: al menos ${MIN_MESSAGE} caracteres.`)
      return
    }
    if (contact.trim().length === 0) {
      setError('Dejá un método de contacto.')
      return
    }
    try {
      await create.mutateAsync({ message: message.trim(), contact: contact.trim() })
      toast.success('¡Petición enviada! El equipo la va a revisar.')
      setMessage('')
      setContact('')
    } catch (err) {
      if (err instanceof ApiError && err.status === 409) {
        toast.error(err.message)
      } else {
        setError(err instanceof ApiError ? err.message : 'No se pudo enviar la petición.')
      }
    }
  }

  return (
    <section className="mb-6 space-y-3 rounded-xl border border-leaf-300 bg-cream-100 p-4 shadow-soft ring-1 ring-leaf-200 sm:p-5">
      <div>
        <h2 className="font-display text-lg font-semibold text-leaf-700">🌱 Convertite en Grow</h2>
        <p className="mt-1 text-sm text-ink-soft">
          Los Grow pueden publicar artículos y guías, sumar portada y datos de comercio. Contanos
          por qué querés serlo y dejá un contacto; un moderador va a revisar tu pedido.
        </p>
      </div>

      {pending ? (
        <div className="rounded-lg bg-leaf-100/60 p-3 text-sm text-ink">
          <p className="font-semibold text-leaf-700">Tu solicitud está en revisión ⏳</p>
          <p className="mt-1 whitespace-pre-wrap break-words text-ink-soft">«{latest.message}»</p>
          <p className="mt-1 text-xs text-ink-faint">Enviada el {formatDate(latest.createdAt)}</p>
        </div>
      ) : (
        <form onSubmit={handleSubmit} className="space-y-3">
          {latest?.status === 'REJECTED' && (
            <p className="text-sm text-ink-soft">
              Tu última solicitud fue rechazada. Podés intentar de nuevo con más información.
            </p>
          )}
          <div>
            <div className="mb-1 flex items-center justify-between">
              <span className="field-label">Tu mensaje</span>
              <span className="text-xs text-ink-faint">
                {message.length}/{MAX_MESSAGE}
              </span>
            </div>
            <textarea
              value={message}
              onChange={(e) => setMessage(e.target.value.slice(0, MAX_MESSAGE))}
              rows={4}
              placeholder="Contá tu experiencia cultivando, qué te gustaría aportar al foro, si tenés un growshop…"
              className="input"
            />
          </div>
          <label className="block">
            <span className="field-label">Método de contacto</span>
            <input
              type="text"
              value={contact}
              onChange={(e) => setContact(e.target.value.slice(0, MAX_CONTACT))}
              placeholder="WhatsApp, Instagram, email… como prefieras que te contacten"
              className="input"
            />
          </label>

          {error && <p className="text-sm text-red-700">{error}</p>}

          <button type="submit" disabled={create.isPending} className="btn-primary">
            {create.isPending ? 'Enviando…' : 'Enviar solicitud'}
          </button>
        </form>
      )}
    </section>
  )
}
