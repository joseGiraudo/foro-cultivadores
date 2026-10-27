import { useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { resetPassword } from '../api/auth'
import { ApiError } from '../api/client'

export function ResetPasswordPage() {
  const [params] = useSearchParams()
  const token = params.get('token')

  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [done, setDone] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    if (password.length < 8) {
      setError('La contraseña debe tener al menos 8 caracteres.')
      return
    }
    if (password !== confirm) {
      setError('Las contraseñas no coinciden.')
      return
    }
    setSubmitting(true)
    try {
      await resetPassword(token ?? '', password)
      setDone(true)
    } catch (err) {
      if (err instanceof ApiError && err.status === 400) {
        setError('El enlace es inválido o venció. Pedí uno nuevo.')
      } else {
        setError('No se pudo cambiar la contraseña. Intentá de nuevo.')
      }
    } finally {
      setSubmitting(false)
    }
  }

  if (!token) {
    return (
      <div className="mx-auto max-w-sm">
        <h1 className="mb-4 text-2xl font-semibold text-ink">Restablecer contraseña</h1>
        <p className="text-sm text-red-700">
          Falta el enlace de recuperación.{' '}
          <Link to="/forgot-password" className="font-semibold text-leaf-700 hover:underline">
            Pedí uno nuevo
          </Link>
          .
        </p>
      </div>
    )
  }

  if (done) {
    return (
      <div className="mx-auto max-w-sm">
        <h1 className="mb-4 text-2xl font-semibold text-ink">Listo</h1>
        <div className="rounded-lg border border-leaf-300 bg-leaf-100 p-4 text-sm text-ink-soft">
          <p>Tu contraseña se cambió. Ya podés ingresar con la nueva.</p>
          <Link to="/login" className="mt-3 inline-block font-semibold text-leaf-700 hover:underline">
            Ir a ingresar
          </Link>
        </div>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-sm">
      <h1 className="mb-4 text-2xl font-semibold text-ink">Nueva contraseña</h1>
      <form onSubmit={handleSubmit} className="space-y-4" noValidate>
        <label className="block">
          <span className="field-label">Nueva contraseña</span>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="new-password"
            className="input"
          />
        </label>
        <label className="block">
          <span className="field-label">Repetir contraseña</span>
          <input
            type="password"
            value={confirm}
            onChange={(e) => setConfirm(e.target.value)}
            autoComplete="new-password"
            className="input"
          />
        </label>

        {error && <p className="text-sm text-red-700">{error}</p>}

        <button type="submit" disabled={submitting} className="btn-primary w-full">
          {submitting ? 'Guardando…' : 'Cambiar contraseña'}
        </button>
      </form>
    </div>
  )
}
