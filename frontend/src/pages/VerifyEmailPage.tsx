import { useEffect, useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { resendVerification, verifyEmail } from '../api/auth'

type Status = 'verifying' | 'success' | 'error' | 'idle'

export function VerifyEmailPage() {
  const [params] = useSearchParams()
  const token = params.get('token')
  const [status, setStatus] = useState<Status>(token ? 'verifying' : 'idle')

  // Si hay token en la URL, verificarlo al montar.
  useEffect(() => {
    if (!token) return
    let cancelled = false
    verifyEmail(token)
      .then(() => !cancelled && setStatus('success'))
      .catch(() => !cancelled && setStatus('error'))
    return () => {
      cancelled = true
    }
  }, [token])

  return (
    <div className="mx-auto max-w-sm">
      <h1 className="mb-4 text-2xl font-semibold text-ink">Verificar email</h1>

      {status === 'verifying' && <p className="text-sm text-ink-soft">Verificando tu email…</p>}

      {status === 'success' && (
        <div className="rounded-lg border border-leaf-300 bg-leaf-100 p-4 text-sm text-ink-soft">
          <p>¡Tu email quedó verificado! Ya podés ingresar.</p>
          <Link to="/login" className="mt-3 inline-block font-semibold text-leaf-700 hover:underline">
            Ir a ingresar
          </Link>
        </div>
      )}

      {(status === 'error' || status === 'idle') && (
        <>
          {status === 'error' && (
            <p className="mb-3 text-sm text-red-700">
              El enlace es inválido o venció. Pedí uno nuevo con tu email.
            </p>
          )}
          <ResendForm />
        </>
      )}
    </div>
  )
}

function ResendForm() {
  const [email, setEmail] = useState('')
  const [sent, setSent] = useState(false)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setSubmitting(true)
    try {
      await resendVerification(email)
    } finally {
      // Respuesta genérica: no revelamos si el email existe.
      setSent(true)
      setSubmitting(false)
    }
  }

  if (sent) {
    return (
      <p className="text-sm text-ink-soft">
        Si el email corresponde a una cuenta sin verificar, te enviamos el enlace. Revisá tu casilla.
      </p>
    )
  }

  return (
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
        {submitting ? 'Enviando…' : 'Reenviar verificación'}
      </button>
    </form>
  )
}
