import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ApiError } from '../api/client'
import { useAuth } from '../auth/useAuth'

const USERNAME_RE = /^[a-zA-Z0-9_]{3,30}$/
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

// Validaciones espejo del backend (feedback rápido; el backend igual valida).
function validate(username: string, email: string, password: string): Record<string, string> {
  const errors: Record<string, string> = {}
  if (!USERNAME_RE.test(username)) {
    errors.username = 'Entre 3 y 30 caracteres: letras, números o guion bajo.'
  }
  if (!EMAIL_RE.test(email)) {
    errors.email = 'Ingresá un email válido.'
  }
  if (password.length < 8) {
    errors.password = 'La contraseña debe tener al menos 8 caracteres.'
  }
  return errors
}

export function RegisterPage() {
  const { register } = useAuth()
  const navigate = useNavigate()

  const [username, setUsername] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [formError, setFormError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setFormError(null)

    const clientErrors = validate(username, email, password)
    if (Object.keys(clientErrors).length > 0) {
      setFieldErrors(clientErrors)
      return
    }
    setFieldErrors({})
    setSubmitting(true)
    try {
      await register(username, email, password)
      navigate('/', { replace: true })
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.status === 409) {
          setFormError(err.detail ?? 'El usuario o el email ya están en uso.')
        } else if (err.status === 400 && err.fieldErrors) {
          setFieldErrors(err.fieldErrors)
        } else {
          setFormError(err.message)
        }
      } else {
        setFormError('No se pudo registrar. Intentá de nuevo.')
      }
    } finally {
      setSubmitting(false)
    }
  }

  const inputClass = (field: string) =>
    `w-full rounded-md border px-3 py-2 focus:outline-none ${
      fieldErrors[field] ? 'border-red-400 focus:border-red-500' : 'border-gray-300 focus:border-green-600'
    }`

  return (
    <div className="mx-auto max-w-sm">
      <h1 className="mb-4 text-2xl font-bold text-gray-900">Registro</h1>
      <form onSubmit={handleSubmit} className="space-y-4" noValidate>
        <label className="block">
          <span className="mb-1 block text-sm font-medium text-gray-700">Usuario</span>
          <input
            type="text"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            autoComplete="username"
            className={inputClass('username')}
          />
          {fieldErrors.username && <p className="mt-1 text-sm text-red-700">{fieldErrors.username}</p>}
        </label>
        <label className="block">
          <span className="mb-1 block text-sm font-medium text-gray-700">Email</span>
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            autoComplete="email"
            className={inputClass('email')}
          />
          {fieldErrors.email && <p className="mt-1 text-sm text-red-700">{fieldErrors.email}</p>}
        </label>
        <label className="block">
          <span className="mb-1 block text-sm font-medium text-gray-700">Contraseña</span>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="new-password"
            className={inputClass('password')}
          />
          {fieldErrors.password && <p className="mt-1 text-sm text-red-700">{fieldErrors.password}</p>}
        </label>

        {formError && <p className="text-sm text-red-700">{formError}</p>}

        <button
          type="submit"
          disabled={submitting}
          className="w-full rounded-md bg-green-700 px-4 py-2 font-medium text-white hover:bg-green-800 disabled:opacity-50"
        >
          {submitting ? 'Creando cuenta…' : 'Crear cuenta'}
        </button>
      </form>

      <p className="mt-4 text-sm text-gray-600">
        ¿Ya tenés cuenta?{' '}
        <Link to="/login" className="font-medium text-green-700 hover:underline">
          Ingresá
        </Link>
      </p>
    </div>
  )
}
