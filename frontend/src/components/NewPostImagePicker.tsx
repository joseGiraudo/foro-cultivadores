import { useEffect, useMemo, useRef, useState, type ChangeEvent } from 'react'

// Límites espejo del backend (forum.images). El backend valida por contenido real igual;
// esto es solo para feedback rápido antes de enviar.
const MAX_FILE_SIZE = 5 * 1024 * 1024
const MAX_PER_POST = 10
const ALLOWED_TYPES = ['image/jpeg', 'image/png', 'image/webp']

interface NewPostImagePickerProps {
  files: File[]
  onChange: (files: File[]) => void
}

// Selector de imágenes para la creación de una consulta: acumula File[] en memoria (no sube nada
// todavía; las fotos viajan junto al post al publicar). Permite previsualizar y quitar antes de enviar.
export function NewPostImagePicker({ files, onChange }: NewPostImagePickerProps) {
  const inputRef = useRef<HTMLInputElement>(null)
  const [error, setError] = useState<string | null>(null)

  // URLs de preview derivadas de los File (sin setState). El efecto solo las revoca cuando cambian
  // o al desmontar, para no filtrar memoria.
  const previews = useMemo(() => files.map((file) => URL.createObjectURL(file)), [files])
  useEffect(() => () => previews.forEach((url) => URL.revokeObjectURL(url)), [previews])

  const remaining = MAX_PER_POST - files.length

  function handleSelect(event: ChangeEvent<HTMLInputElement>) {
    const selected = Array.from(event.target.files ?? [])
    if (inputRef.current) inputRef.current.value = '' // permitir re-seleccionar los mismos archivos
    if (selected.length === 0) return

    setError(null)
    if (selected.length > remaining) {
      setError(
        `Seleccionaste ${selected.length} imágenes pero solo podés agregar ${remaining} más ` +
          `(máximo ${MAX_PER_POST} por publicación).`,
      )
      return
    }
    for (const file of selected) {
      if (!ALLOWED_TYPES.includes(file.type)) {
        setError(`"${file.name}": tipo no permitido (usá JPEG, PNG o WebP).`)
        return
      }
      if (file.size > MAX_FILE_SIZE) {
        setError(`"${file.name}": supera el tamaño máximo (5 MB).`)
        return
      }
    }
    onChange([...files, ...selected])
  }

  function removeAt(index: number) {
    onChange(files.filter((_, i) => i !== index))
  }

  return (
    <div>
      <span className="field-label">Imágenes (opcional)</span>
      <div className="mb-2 flex items-center gap-3">
        <input
          ref={inputRef}
          type="file"
          accept={ALLOWED_TYPES.join(',')}
          multiple
          onChange={handleSelect}
          disabled={remaining <= 0}
          className="text-sm"
        />
      </div>
      <p className="mb-2 text-xs text-ink-faint">
        {files.length}/{MAX_PER_POST} imágenes · se muestran en galería en el orden en que las agregás ·
        JPEG, PNG o WebP · hasta 5 MB cada una.
      </p>
      {error && <p className="mb-2 text-sm text-red-700">{error}</p>}

      {files.length > 0 && (
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
          {files.map((file, index) => (
            <div key={`${file.name}-${index}`} className="rounded-lg border border-sand-200 p-2">
              <img
                src={previews[index]}
                alt=""
                className="mb-2 aspect-square w-full rounded object-cover"
              />
              <button
                type="button"
                onClick={() => removeAt(index)}
                className="text-xs font-medium text-red-700 hover:underline"
              >
                Quitar
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
