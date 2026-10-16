import { useState, type FormEvent } from 'react'
import { ApiError } from '../api/client'

interface CommentFormProps {
  // Envía el contenido. Debe lanzar en caso de error para mostrarlo acá.
  onSubmit: (content: string) => Promise<void>
  submitLabel: string
  initialValue?: string
  placeholder?: string
  onCancel?: () => void
  autoFocus?: boolean
  // Si es true, limpia el textarea tras enviar con éxito (útil en el form de nivel superior,
  // que no se desmonta). En respuesta/edición el form se desmonta, así que da igual.
  clearOnSuccess?: boolean
}

export function CommentForm({
  onSubmit,
  submitLabel,
  initialValue = '',
  placeholder = 'Escribí un comentario…',
  onCancel,
  autoFocus,
  clearOnSuccess,
}: CommentFormProps) {
  const [content, setContent] = useState(initialValue)
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (content.trim().length === 0) return
    setError(null)
    setSubmitting(true)
    try {
      await onSubmit(content)
      if (clearOnSuccess) setContent('')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo enviar el comentario.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-2">
      <textarea
        value={content}
        onChange={(e) => setContent(e.target.value)}
        placeholder={placeholder}
        rows={3}
        maxLength={5000}
        autoFocus={autoFocus}
        className="input p-2 text-sm"
      />
      {error && <p className="text-sm text-red-700">{error}</p>}
      <div className="flex gap-2">
        <button
          type="submit"
          disabled={submitting || content.trim().length === 0}
          className="btn-primary px-3 py-1.5 text-sm"
        >
          {submitting ? 'Enviando…' : submitLabel}
        </button>
        {onCancel && (
          <button type="button" onClick={onCancel} className="btn-secondary px-3 py-1.5 text-sm">

            Cancelar
          </button>
        )}
      </div>
    </form>
  )
}
