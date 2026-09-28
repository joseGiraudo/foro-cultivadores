import { ApiError } from '../api/client'

export function Loading({ label = 'Cargando…' }: { label?: string }) {
  return <p className="py-8 text-center text-gray-500">{label}</p>
}

export function EmptyState({ message }: { message: string }) {
  return (
    <div className="rounded-md border border-dashed border-gray-300 py-10 text-center text-gray-500">
      {message}
    </div>
  )
}

// Muestra un error de forma consistente. Traduce el status de ApiError a un mensaje útil.
export function ErrorState({ error, onRetry }: { error: unknown; onRetry?: () => void }) {
  let message = 'Ocurrió un error inesperado.'
  if (error instanceof ApiError) {
    message =
      error.status === 404
        ? 'No encontrado.'
        : error.status === 403
          ? 'No tenés permiso para ver esto.'
          : (error.detail ?? error.message)
  }
  return (
    <div className="rounded-md border border-red-200 bg-red-50 p-4 text-red-800">
      <p className="font-medium">{message}</p>
      {onRetry && (
        <button
          type="button"
          onClick={onRetry}
          className="mt-2 text-sm font-medium text-red-700 underline"
        >
          Reintentar
        </button>
      )}
    </div>
  )
}
