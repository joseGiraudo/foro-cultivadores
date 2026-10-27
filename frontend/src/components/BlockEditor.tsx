import { useRef, useState, type ChangeEvent } from 'react'
import {
  blocksToMarkdown,
  makeImageBlock,
  makeTextBlock,
  markdownToBlocks,
  type Block,
} from '../lib/articleBlocks'
import { resolveImageUrl } from '../lib/format'
import { MarkdownContent } from './MarkdownContent'

// Límites espejo del backend (forum.images). El backend valida por contenido real igual.
const MAX_FILE_SIZE = 5 * 1024 * 1024
const ALLOWED_TYPES = ['image/jpeg', 'image/png', 'image/webp']

interface BlockEditorProps {
  // Markdown inicial (se parsea a bloques una sola vez; usar `key` para reinicializar por post).
  initialValue: string
  onChange: (markdown: string) => void
  // Sube una imagen y devuelve su URL (/images/{id}). Si no se pasa, no se puede agregar imágenes
  // (p. ej. al crear, cuando el post todavía no existe).
  onUploadImage?: (file: File) => Promise<string>
}

// Editor visual por bloques para artículos/guías: párrafos de texto y bloques de imagen en orden,
// reordenables. El contenido se serializa a Markdown (lo que ya guarda/renderiza el backend).
export function BlockEditor({ initialValue, onChange, onUploadImage }: BlockEditorProps) {
  const [blocks, setBlocks] = useState<Block[]>(() => markdownToBlocks(initialValue))
  const [preview, setPreview] = useState(false)
  const [uploading, setUploading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const fileInputRef = useRef<HTMLInputElement>(null)

  function commit(next: Block[]) {
    setBlocks(next)
    onChange(blocksToMarkdown(next))
  }

  function updateText(id: string, text: string) {
    commit(blocks.map((b) => (b.id === id && b.type === 'text' ? { ...b, text } : b)))
  }

  function move(index: number, direction: -1 | 1) {
    const target = index + direction
    if (target < 0 || target >= blocks.length) return
    const next = [...blocks]
    ;[next[index], next[target]] = [next[target], next[index]]
    commit(next)
  }

  function removeBlock(index: number) {
    // Solo se quita del contenido; el archivo subido queda (inofensivo) para no romper la
    // publicación si el autor quita un bloque y no guarda. La limpieza de huérfanos es aparte.
    commit(blocks.filter((_, i) => i !== index))
  }

  function addTextBlock() {
    commit([...blocks, makeTextBlock()])
  }

  async function handleFiles(event: ChangeEvent<HTMLInputElement>) {
    const files = Array.from(event.target.files ?? [])
    if (fileInputRef.current) fileInputRef.current.value = ''
    if (files.length === 0 || !onUploadImage) return

    setError(null)
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

    setUploading(true)
    try {
      const uploaded: Block[] = []
      for (const file of files) {
        const url = await onUploadImage(file)
        uploaded.push(makeImageBlock(url))
      }
      commit([...blocks, ...uploaded])
    } catch {
      setError('No se pudo subir alguna imagen. Probá de nuevo.')
    } finally {
      setUploading(false)
    }
  }

  const tabClass = (active: boolean) =>
    `px-3 py-1 text-sm font-semibold rounded-t-md ${
      active ? 'bg-cream-50 border border-b-0 border-sand-200 text-ink' : 'text-ink-faint'
    }`

  return (
    <div>
      <div className="flex gap-1">
        <button type="button" className={tabClass(!preview)} onClick={() => setPreview(false)}>
          Editar
        </button>
        <button type="button" className={tabClass(preview)} onClick={() => setPreview(true)}>
          Vista previa
        </button>
      </div>

      {preview ? (
        <div className="min-h-40 rounded-md rounded-tl-none border border-sand-200 bg-cream-50 p-3">
          {blocks.some((b) => (b.type === 'text' ? b.text.trim() : true)) ? (
            <MarkdownContent content={blocksToMarkdown(blocks)} />
          ) : (
            <p className="text-sm text-ink-faint">Nada que previsualizar todavía.</p>
          )}
        </div>
      ) : (
        <div className="space-y-2 rounded-md rounded-tl-none border border-sand-200 bg-cream-50 p-3">
          {blocks.map((block, index) => (
            <div
              key={block.id}
              className="flex gap-2 rounded-lg border border-sand-200 bg-cream-100 p-2"
            >
              <div className="min-w-0 flex-1">
                {block.type === 'text' ? (
                  <textarea
                    value={block.text}
                    onChange={(e) => updateText(block.id, e.target.value)}
                    rows={Math.max(3, block.text.split('\n').length + 1)}
                    placeholder="Escribí un párrafo… (podés usar Markdown: **negrita**, listas, ## títulos)"
                    className="w-full resize-y rounded-md border border-sand-200 bg-cream-50 p-2 text-sm text-ink focus:border-leaf-400 focus:outline-none"
                  />
                ) : (
                  <img
                    src={resolveImageUrl(block.url)}
                    alt={block.alt}
                    className="mx-auto max-h-64 rounded-md object-contain"
                  />
                )}
              </div>
              <div className="flex shrink-0 flex-col gap-1">
                <button
                  type="button"
                  onClick={() => move(index, -1)}
                  disabled={index === 0}
                  aria-label="Subir bloque"
                  className="rounded border border-sand-200 px-2 py-0.5 text-sm text-ink-soft hover:bg-leaf-100 disabled:opacity-40"
                >
                  ↑
                </button>
                <button
                  type="button"
                  onClick={() => move(index, 1)}
                  disabled={index === blocks.length - 1}
                  aria-label="Bajar bloque"
                  className="rounded border border-sand-200 px-2 py-0.5 text-sm text-ink-soft hover:bg-leaf-100 disabled:opacity-40"
                >
                  ↓
                </button>
                <button
                  type="button"
                  onClick={() => removeBlock(index)}
                  aria-label="Eliminar bloque"
                  className="rounded border border-red-200 px-2 py-0.5 text-sm text-red-700 hover:bg-red-50"
                >
                  ✕
                </button>
              </div>
            </div>
          ))}

          {error && <p className="text-sm text-red-700">{error}</p>}

          <div className="flex flex-wrap items-center gap-2 pt-1">
            <button
              type="button"
              onClick={addTextBlock}
              className="rounded-lg border border-sand-200 px-3 py-1 text-sm font-semibold text-ink-soft hover:bg-leaf-100 hover:text-leaf-700"
            >
              + Párrafo
            </button>
            {onUploadImage && (
              <>
                <button
                  type="button"
                  onClick={() => fileInputRef.current?.click()}
                  disabled={uploading}
                  className="rounded-lg border border-sand-200 px-3 py-1 text-sm font-semibold text-ink-soft hover:bg-leaf-100 hover:text-leaf-700 disabled:opacity-50"
                >
                  {uploading ? 'Subiendo…' : '+ Imagen'}
                </button>
                <input
                  ref={fileInputRef}
                  type="file"
                  accept={ALLOWED_TYPES.join(',')}
                  multiple
                  onChange={handleFiles}
                  className="hidden"
                />
              </>
            )}
          </div>

          {!onUploadImage && (
            <p className="text-xs text-ink-faint">
              💡 Para sumar imágenes, primero creá la publicación. Después, al editarla, vas a poder
              subirlas y ubicarlas entre los párrafos.
            </p>
          )}
        </div>
      )}
    </div>
  )
}
