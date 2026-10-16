interface PaginationProps {
  page: number // página actual (base 0)
  totalPages: number
  onChange: (page: number) => void
}

export function Pagination({ page, totalPages, onChange }: PaginationProps) {
  if (totalPages <= 1) return null

  const buttonClass =
    'rounded-lg border border-sand-200 bg-cream-100 px-3 py-1 text-sm font-semibold text-ink-soft transition disabled:opacity-40 hover:bg-leaf-100 hover:text-leaf-700 disabled:hover:bg-cream-100 disabled:hover:text-ink-soft'

  return (
    <div className="mt-6 flex items-center justify-center gap-4">
      <button
        type="button"
        className={buttonClass}
        disabled={page <= 0}
        onClick={() => onChange(page - 1)}
      >
        ← Anterior
      </button>
      <span className="text-sm text-ink-soft">
        Página {page + 1} de {totalPages}
      </span>
      <button
        type="button"
        className={buttonClass}
        disabled={page >= totalPages - 1}
        onClick={() => onChange(page + 1)}
      >
        Siguiente →
      </button>
    </div>
  )
}
