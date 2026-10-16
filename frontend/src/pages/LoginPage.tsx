import { useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { ApiError } from '../api/client'
import { useAuth } from '../auth/useAuth'

interface LocationState {
  from?: { pathname: string }
}

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const from = (location.state as LocationState | null)?.from?.pathname ?? '/'

  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await login(username, password)
      navigate(from, { replace: true })
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo ingresar. Intentá de nuevo.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="mx-auto max-w-sm">
      <h1 className="mb-4 text-2xl font-semibold text-ink">Ingresar</h1>
      <form onSubmit={handleSubmit} className="space-y-4">
        <label className="block">
          <span className="field-label">Usuario</span>
          <input
            type="text"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            autoComplete="username"
            required
            className="input"
          />
        </label>
        <label className="block">
          <span className="field-label">Contraseña</span>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="current-password"
            required
            className="input"
          />
        </label>

        {error && <p className="text-sm text-red-700">{error}</p>}

        <button type="submit" disabled={submitting} className="btn-primary w-full">
          {submitting ? 'Ingresando…' : 'Ingresar'}
        </button>
      </form>

      <p className="mt-4 text-sm text-ink-soft">
        ¿No tenés cuenta?{' '}
        <Link to="/register" className="font-semibold text-leaf-700 hover:underline">
          Registrate
        </Link>
      </p>
    </div>
  )
}
