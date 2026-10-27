import { useCategories } from '../hooks/useCategories'

interface CategoryPickerProps {
  selected: number[]
  onChange: (ids: number[]) => void
  max?: number
}

// Selector de categorías (entre 1 y `max`, por defecto 4). Al llegar al tope deshabilita las no
// seleccionadas. Si hay muchas, el área scrollea (alto acotado) en vez de crecer sin límite.
export function CategoryPicker({ selected, onChange, max = 4 }: CategoryPickerProps) {
  const { data: categories, isLoading } = useCategories()

  if (isLoading) return <p className="text-sm text-ink-faint">Cargando categorías…</p>
  if (!categories) return null

  function toggle(id: number) {
    if (selected.includes(id)) {
      onChange(selected.filter((current) => current !== id))
    } else if (selected.length < max) {
      onChange([...selected, id])
    }
  }

  const atCap = selected.length >= max

  return (
    <div>
      <div className="flex max-h-44 flex-wrap gap-2 overflow-y-auto rounded-lg border border-sand-200 bg-cream-50 p-2">
        {categories.map((category) => {
          const active = selected.includes(category.id)
          const disabled = !active && atCap
          return (
            <button
              key={category.id}
              type="button"
              onClick={() => toggle(category.id)}
              disabled={disabled}
              aria-pressed={active}
              className={`inline-flex items-center gap-1.5 rounded-full border px-3 py-1 text-sm transition ${
                active
                  ? 'border-leaf-600 bg-leaf-600 font-semibold text-white'
                  : 'border-sand-200 text-ink-soft hover:bg-leaf-100 hover:text-leaf-700'
              } disabled:cursor-not-allowed disabled:opacity-40`}
            >
              <span
                aria-hidden="true"
                className={`flex h-4 w-4 items-center justify-center rounded-full border text-[10px] leading-none ${
                  active ? 'border-white bg-white text-leaf-700' : 'border-sand-300 text-transparent'
                }`}
              >
                ✓
              </span>
              {category.name}
            </button>
          )
        })}
      </div>

      <p className="mt-2 text-xs text-ink-faint">
        {selected.length} de {max} elegidas
      </p>
    </div>
  )
}
