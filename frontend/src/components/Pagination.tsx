interface PaginationProps {
  page: number // página actual (base 0)
  totalPages: number
  onChange: (page: number) => void
}

export function Pagination({ page, totalPages, onChange }: PaginationProps) {
  if (totalPages <= 1) return null

  const buttonClass =
    'rounded-md border border-gray-300 px-3 py-1 text-sm font-medium disabled:opacity-40 hover:bg-gray-100 disabled:hover:bg-transparent'

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
      <span className="text-sm text-gray-600">
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
