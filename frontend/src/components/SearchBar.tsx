import type { FormEvent } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'

/**
 * Barra de búsqueda global (vive en el Navbar). Al enviar (Enter) navega a /search?q=…,
 * así los resultados quedan en la URL (compartibles). Input no controlado: se re-sincroniza
 * con la query actual vía `key` (sin efectos) para reflejar navegaciones atrás/adelante.
 */
export function SearchBar({ onNavigate }: { onNavigate?: () => void }) {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const current = params.get('q') ?? ''

  function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const value = ((new FormData(e.currentTarget).get('q') as string | null) ?? '').trim()
    if (!value) return
    onNavigate?.()
    navigate(`/search?q=${encodeURIComponent(value)}`)
  }

  return (
    <form onSubmit={handleSubmit} role="search" className="relative w-full">
      <span className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-ink-faint">
        🔍
      </span>
      <input
        key={current}
        type="search"
        name="q"
        defaultValue={current}
        placeholder="Buscar en el foro…"
        aria-label="Buscar publicaciones"
        className="w-full rounded-lg border border-sand-200 bg-cream-50 py-2 pl-9 pr-3 text-sm text-ink shadow-soft transition focus:border-leaf-400 focus:outline-none"
      />
    </form>
  )
}
