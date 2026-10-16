import { useState } from 'react'
import { MarkdownContent } from './MarkdownContent'

interface MarkdownEditorProps {
  value: string
  onChange: (value: string) => void
  placeholder?: string
  rows?: number
}

// Editor de Markdown con pestañas Escribir / Vista previa (reusa MarkdownContent para el render).
export function MarkdownEditor({ value, onChange, placeholder, rows = 12 }: MarkdownEditorProps) {
  const [preview, setPreview] = useState(false)

  const tabClass = (active: boolean) =>
    `px-3 py-1 text-sm font-semibold rounded-t-md ${
      active ? 'bg-cream-50 border border-b-0 border-sand-200 text-ink' : 'text-ink-faint'
    }`

  return (
    <div>
      <div className="flex gap-1">
        <button type="button" className={tabClass(!preview)} onClick={() => setPreview(false)}>
          Escribir
        </button>
        <button type="button" className={tabClass(preview)} onClick={() => setPreview(true)}>
          Vista previa
        </button>
      </div>
      {preview ? (
        <div className="min-h-40 rounded-md rounded-tl-none border border-sand-200 bg-cream-50 p-3">
          {value.trim() ? (
            <MarkdownContent content={value} />
          ) : (
            <p className="text-sm text-ink-faint">Nada que previsualizar todavía.</p>
          )}
        </div>
      ) : (
        <textarea
          value={value}
          onChange={(e) => onChange(e.target.value)}
          placeholder={placeholder}
          rows={rows}
          className="w-full rounded-md rounded-tl-none border border-sand-200 bg-cream-50 p-3 font-mono text-sm text-ink transition focus:border-leaf-400 focus:outline-none"
        />
      )}
    </div>
  )
}
