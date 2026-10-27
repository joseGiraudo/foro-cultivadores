import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { resendVerification } from '../api/auth'
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

  const [username, setUsername] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [formError, setFormError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  // Email con el que se registró, para mostrar el aviso de verificación y permitir reenviar.
  const [registeredEmail, setRegisteredEmail] = useState<string | null>(null)

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
      setRegisteredEmail(email)
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

  // Base `.input`; en error, las utilidades sueltas ganan (utilities > components en v4).
  const inputClass = (field: string) =>
    `input ${fieldErrors[field] ? 'border-red-400 focus:border-red-500' : ''}`

  // Tras registrarse: aviso de verificación (no se inicia sesión hasta confirmar el email).
  if (registeredEmail) {
    return (
      <div className="mx-auto max-w-sm">
        <h1 className="mb-4 text-2xl font-semibold text-ink">Revisá tu email</h1>
        <div className="rounded-lg border border-leaf-300 bg-leaf-100 p-4 text-sm text-ink-soft">
          <p>
            Te enviamos un email a <strong className="text-ink">{registeredEmail}</strong> para
            verificar tu cuenta.
          </p>
          <p className="mt-2">Hacé clic en el enlace del email para activarla y poder ingresar.</p>
        </div>
        <button
          type="button"
          onClick={() => void resendVerification(registeredEmail).catch(() => undefined)}
          className="btn-secondary mt-4 w-full"
        >
          Reenviar email
        </button>
        <p className="mt-4 text-center text-sm text-ink-soft">
          <Link to="/login" className="font-semibold text-leaf-700 hover:underline">
            Ir a ingresar
          </Link>
        </p>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-sm">
      <h1 className="mb-4 text-2xl font-semibold text-ink">Registro</h1>
      <form onSubmit={handleSubmit} className="space-y-4" noValidate>
        <label className="block">
          <span className="field-label">Usuario</span>
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
          <span className="field-label">Email</span>
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
          <span className="field-label">Contraseña</span>
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

        <button type="submit" disabled={submitting} className="btn-primary w-full">
          {submitting ? 'Creando cuenta…' : 'Crear cuenta'}
        </button>
      </form>

      <p className="mt-4 text-sm text-ink-soft">
        ¿Ya tenés cuenta?{' '}
        <Link to="/login" className="font-semibold text-leaf-700 hover:underline">
          Ingresá
        </Link>
      </p>
    </div>
  )
}
