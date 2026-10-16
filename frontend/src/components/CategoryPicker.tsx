import { useCategories } from '../hooks/useCategories'

interface CategoryPickerProps {
  selected: number[]
  onChange: (ids: number[]) => void
  max?: number
}

// Selector de categorías (entre 1 y `max`, por defecto 3). Al llegar al tope,
// deshabilita las no seleccionadas.
export function CategoryPicker({ selected, onChange, max = 3 }: CategoryPickerProps) {
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

  return (
    <div className="flex flex-wrap gap-2">
      {categories.map((category) => {
        const active = selected.includes(category.id)
        const disabled = !active && selected.length >= max
        return (
          <button
            key={category.id}
            type="button"
            onClick={() => toggle(category.id)}
            disabled={disabled}
            className={`rounded-full border px-3 py-1 text-sm transition ${
              active
                ? 'border-leaf-600 bg-leaf-100 text-leaf-700'
                : 'border-sand-200 text-ink-soft hover:bg-leaf-100 hover:text-leaf-700'
            } disabled:cursor-not-allowed disabled:opacity-40`}
          >
            {category.name}
          </button>
        )
      })}
    </div>
  )
}
