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
    `px-3 py-1 text-sm font-medium rounded-t-md ${
      active ? 'bg-white border border-b-0 border-gray-300 text-gray-900' : 'text-gray-500'
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
        <div className="min-h-40 rounded-md rounded-tl-none border border-gray-300 bg-white p-3">
          {value.trim() ? (
            <MarkdownContent content={value} />
          ) : (
            <p className="text-sm text-gray-400">Nada que previsualizar todavía.</p>
          )}
        </div>
      ) : (
        <textarea
          value={value}
          onChange={(e) => onChange(e.target.value)}
          placeholder={placeholder}
          rows={rows}
          className="w-full rounded-md rounded-tl-none border border-gray-300 p-3 font-mono text-sm focus:border-green-600 focus:outline-none"
        />
      )}
    </div>
  )
}
