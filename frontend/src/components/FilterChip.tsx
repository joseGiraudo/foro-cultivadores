import type { ReactNode } from 'react'

/**
 * Píldora de filtro clickeable (toggle). Por defecto resalta en verde marca al activarse;
 * `activeClass` permite un color propio (p. ej. el semántico de cada tipo de publicación).
 */
export function FilterChip({
  active,
  activeClass,
  onClick,
  children,
}: {
  active: boolean
  activeClass?: string
  onClick: () => void
  children: ReactNode
}) {
  const base = 'rounded-full border px-3 py-1.5 text-sm font-medium transition'
  const inactive = 'border-sand-200 bg-cream-100 text-ink-soft hover:border-leaf-300 hover:text-leaf-700'
  const activeDefault = 'border-leaf-600 bg-leaf-600 text-cream-50 shadow-soft'

  return (
    <button
      type="button"
      onClick={onClick}
      aria-pressed={active}
      className={`${base} ${active ? (activeClass ?? activeDefault) : inactive}`}
    >
      {children}
    </button>
  )
}
