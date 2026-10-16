import { useCallback, useEffect, useState } from 'react'
import type { ImageResponse } from '../api/types'
import { resolveImageUrl } from '../lib/format'

// Galería de imágenes de un post (consultas). Las imágenes ya vienen ordenadas por position.
// Click en una miniatura abre un visor (lightbox) dentro de la página, con navegación
// entre imágenes por teclado o botones.
export function PostGallery({ images }: { images: ImageResponse[] }) {
  const [openIndex, setOpenIndex] = useState<number | null>(null)

  if (images.length === 0) return null

  return (
    <>
      <div className="my-4 grid grid-cols-2 gap-2 sm:grid-cols-3">
        {images.map((image, index) => (
          <button
            key={image.id}
            type="button"
            onClick={() => setOpenIndex(index)}
            className="overflow-hidden rounded-lg focus:outline-none focus:ring-2 focus:ring-leaf-500"
          >
            <img
              src={resolveImageUrl(image.url)}
              alt=""
              loading="lazy"
              className="aspect-square w-full object-cover transition hover:opacity-90"
            />
          </button>
        ))}
      </div>

      {openIndex !== null && (
        <Lightbox
          images={images}
          index={openIndex}
          onIndexChange={setOpenIndex}
          onClose={() => setOpenIndex(null)}
        />
      )}
    </>
  )
}

interface LightboxProps {
  images: ImageResponse[]
  index: number
  onIndexChange: (index: number) => void
  onClose: () => void
}

function Lightbox({ images, index, onIndexChange, onClose }: LightboxProps) {
  const count = images.length
  const goPrev = useCallback(
    () => onIndexChange((index - 1 + count) % count),
    [index, count, onIndexChange],
  )
  const goNext = useCallback(() => onIndexChange((index + 1) % count), [index, count, onIndexChange])

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
      else if (e.key === 'ArrowLeft') goPrev()
      else if (e.key === 'ArrowRight') goNext()
    }
    document.addEventListener('keydown', onKey)
    // Evita el scroll del fondo mientras el visor está abierto.
    const prevOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => {
      document.removeEventListener('keydown', onKey)
      document.body.style.overflow = prevOverflow
    }
  }, [onClose, goPrev, goNext])

  const image = images[index]

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-label="Visor de imágenes"
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 p-4"
      onClick={onClose}
    >
      <button
        type="button"
        onClick={onClose}
        aria-label="Cerrar"
        className="absolute right-4 top-4 flex h-10 w-10 items-center justify-center rounded-full bg-white/10 text-2xl leading-none text-white hover:bg-white/20"
      >
        ×
      </button>

      {count > 1 && (
        <>
          <button
            type="button"
            onClick={(e) => {
              e.stopPropagation()
              goPrev()
            }}
            aria-label="Imagen anterior"
            className="absolute left-4 top-1/2 flex h-11 w-11 -translate-y-1/2 items-center justify-center rounded-full bg-white/10 text-2xl leading-none text-white hover:bg-white/20"
          >
            ‹
          </button>
          <button
            type="button"
            onClick={(e) => {
              e.stopPropagation()
              goNext()
            }}
            aria-label="Imagen siguiente"
            className="absolute right-4 top-1/2 flex h-11 w-11 -translate-y-1/2 items-center justify-center rounded-full bg-white/10 text-2xl leading-none text-white hover:bg-white/20"
          >
            ›
          </button>
        </>
      )}

      <figure className="flex max-h-full max-w-full flex-col items-center" onClick={(e) => e.stopPropagation()}>
        <img
          src={resolveImageUrl(image.url)}
          alt=""
          className="max-h-[85vh] max-w-full rounded object-contain"
        />
        {count > 1 && (
          <figcaption className="mt-3 text-sm text-white/80">
            {index + 1} / {count}
          </figcaption>
        )}
      </figure>
    </div>
  )
}
