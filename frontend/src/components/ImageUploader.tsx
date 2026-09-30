import { useRef, useState, type ChangeEvent } from 'react'
import { ApiError } from '../api/client'
import type { ImageResponse } from '../api/types'
import { useDeleteImage, useUploadImages } from '../hooks/usePostMutations'
import { resolveImageUrl } from '../lib/format'

// Límites espejo del backend (forum.images). El backend valida por contenido real igual;
// esto es solo para feedback rápido.
const MAX_FILE_SIZE = 5 * 1024 * 1024
const MAX_PER_POST = 10
const ALLOWED_TYPES = ['image/jpeg', 'image/png', 'image/webp']

interface ImageUploaderProps {
  postId: number
  images: ImageResponse[]
  // Si se pasa, muestra un botón para insertar la referencia Markdown en el contenido
  // (útil en artículos/guías; en consultas no hace falta porque van en galería).
  onInsertReference?: (image: ImageResponse) => void
}

function humanErrorFromApi(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 413) return 'El archivo supera el tamaño máximo (5 MB).'
    if (error.status === 415) return 'Tipo de archivo no permitido (usá JPEG, PNG o WebP).'
    if (error.status === 409) return `No podés subir más de ${MAX_PER_POST} imágenes.`
    return error.detail ?? error.message
  }
  return 'No se pudo subir la imagen.'
}

export function ImageUploader({ postId, images, onInsertReference }: ImageUploaderProps) {
  const upload = useUploadImages(postId)
  const remove = useDeleteImage(postId)
  const inputRef = useRef<HTMLInputElement>(null)
  const [error, setError] = useState<string | null>(null)

  const remaining = MAX_PER_POST - images.length
  const atLimit = remaining <= 0

  function handleSelect(event: ChangeEvent<HTMLInputElement>) {
    const files = Array.from(event.target.files ?? [])
    if (inputRef.current) inputRef.current.value = '' // permitir re-seleccionar los mismos archivos
    if (files.length === 0) return

    setError(null)
    if (files.length > remaining) {
      setError(
        `Seleccionaste ${files.length} imágenes pero solo podés subir ${remaining} más ` +
          `(máximo ${MAX_PER_POST} por publicación).`,
      )
      return
    }

    // Validación rápida por archivo (el backend igual valida por contenido real).
    for (const file of files) {
      if (!ALLOWED_TYPES.includes(file.type)) {
        setError(`"${file.name}": tipo no permitido (usá JPEG, PNG o WebP).`)
        return
      }
      if (file.size > MAX_FILE_SIZE) {
        setError(`"${file.name}": supera el tamaño máximo (5 MB).`)
        return
      }
    }

    upload.mutate(files, {
      onSuccess: (result) => {
        if (result.failed > 0) {
          const detail = humanErrorFromApi(result.firstError)
          setError(
            result.uploaded > 0
              ? `Se subieron ${result.uploaded} de ${files.length}. ${detail}`
              : detail,
          )
        }
      },
      onError: (err) => setError(humanErrorFromApi(err)),
    })
  }

  return (
    <div>
      <div className="mb-2 flex items-center gap-3">
        <input
          ref={inputRef}
          type="file"
          accept={ALLOWED_TYPES.join(',')}
          multiple
          onChange={handleSelect}
          disabled={atLimit || upload.isPending}
          className="text-sm"
        />
        {upload.isPending && <span className="text-sm text-gray-500">Subiendo…</span>}
      </div>
      <p className="mb-2 text-xs text-gray-500">
        {images.length}/{MAX_PER_POST} imágenes · podés elegir varias a la vez · JPEG, PNG o WebP ·
        hasta 5 MB cada una.
      </p>
      {error && <p className="mb-2 text-sm text-red-700">{error}</p>}

      {images.length > 0 && (
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
          {images.map((image) => (
            <div key={image.id} className="rounded-md border border-gray-200 p-2">
              <img
                src={resolveImageUrl(image.url)}
                alt=""
                className="mb-2 aspect-square w-full rounded object-cover"
              />
              <div className="flex flex-wrap gap-2">
                {onInsertReference && (
                  <button
                    type="button"
                    onClick={() => onInsertReference(image)}
                    className="text-xs font-medium text-green-700 hover:underline"
                  >
                    Insertar en el texto
                  </button>
                )}
                <button
                  type="button"
                  onClick={() => remove.mutate(image.id)}
                  disabled={remove.isPending}
                  className="text-xs font-medium text-red-700 hover:underline disabled:opacity-50"
                >
                  Eliminar
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
