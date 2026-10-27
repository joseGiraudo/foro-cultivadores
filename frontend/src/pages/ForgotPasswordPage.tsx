import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { forgotPassword } from '../api/auth'

export function ForgotPasswordPage() {
  const [email, setEmail] = useState('')
  const [sent, setSent] = useState(false)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setSubmitting(true)
    try {
      await forgotPassword(email)
    } finally {
      // Respuesta genérica: no revelamos si el email existe.
      setSent(true)
      setSubmitting(false)
    }
  }

  return (
    <div className="mx-auto max-w-sm">
      <h1 className="mb-4 text-2xl font-semibold text-ink">Recuperar contraseña</h1>

      {sent ? (
        <div className="rounded-lg border border-leaf-300 bg-leaf-100 p-4 text-sm text-ink-soft">
          <p>
            Si el email corresponde a una cuenta, te enviamos un enlace para elegir una nueva
            contraseña. Revisá tu casilla (el enlace vence en 1 hora).
          </p>
          <Link to="/login" className="mt-3 inline-block font-semibold text-leaf-700 hover:underline">
            Volver a ingresar
          </Link>
        </div>
      ) : (
        <>
          <p className="mb-4 text-sm text-ink-soft">
            Ingresá tu email y te enviamos un enlace para restablecer tu contraseña.
          </p>
          <form onSubmit={handleSubmit} className="space-y-4">
            <label className="block">
              <span className="field-label">Email</span>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                autoComplete="email"
                required
                className="input"
              />
            </label>
            <button type="submit" disabled={submitting} className="btn-primary w-full">
              {submitting ? 'Enviando…' : 'Enviar enlace'}
            </button>
          </form>
          <p className="mt-4 text-sm text-ink-soft">
            <Link to="/login" className="font-semibold text-leaf-700 hover:underline">
              Volver
            </Link>
          </p>
        </>
      )}
    </div>
  )
}
