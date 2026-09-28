import { useRef, useState, type ChangeEvent } from 'react'
import { ApiError } from '../api/client'
import type { ImageResponse } from '../api/types'
import { useDeleteImage, useUploadImage } from '../hooks/usePostMutations'
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
  const upload = useUploadImage(postId)
  const remove = useDeleteImage(postId)
  const inputRef = useRef<HTMLInputElement>(null)
  const [error, setError] = useState<string | null>(null)

  const atLimit = images.length >= MAX_PER_POST

  function handleSelect(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    if (inputRef.current) inputRef.current.value = '' // permitir re-subir el mismo archivo
    if (!file) return

    setError(null)
    if (!ALLOWED_TYPES.includes(file.type)) {
      setError('Tipo de archivo no permitido (usá JPEG, PNG o WebP).')
      return
    }
    if (file.size > MAX_FILE_SIZE) {
      setError('El archivo supera el tamaño máximo (5 MB).')
      return
    }
    upload.mutate(file, { onError: (err) => setError(humanErrorFromApi(err)) })
  }

  return (
    <div>
      <div className="mb-2 flex items-center gap-3">
        <input
          ref={inputRef}
          type="file"
          accept={ALLOWED_TYPES.join(',')}
          onChange={handleSelect}
          disabled={atLimit || upload.isPending}
          className="text-sm"
        />
        {upload.isPending && <span className="text-sm text-gray-500">Subiendo…</span>}
      </div>
      <p className="mb-2 text-xs text-gray-500">
        {images.length}/{MAX_PER_POST} imágenes · JPEG, PNG o WebP · hasta 5 MB.
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
