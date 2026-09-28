import { useAuth } from '../auth/useAuth'

// Aviso persistente para usuarios suspendidos: pueden leer pero cualquier escritura da 403.
export function SuspendedBanner() {
  const { isSuspended } = useAuth()
  if (!isSuspended) return null
  return (
    <div className="border-b border-amber-200 bg-amber-50 px-4 py-2 text-center text-sm text-amber-800">
      Tu cuenta está <strong>suspendida</strong>: podés leer, pero no publicar, comentar ni votar.
    </div>
  )
}
