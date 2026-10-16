import { CategoryPicker } from './CategoryPicker'
import { MarkdownEditor } from './MarkdownEditor'

interface PostFieldsProps {
  title: string
  onTitleChange: (value: string) => void
  categoryIds: number[]
  onCategoriesChange: (ids: number[]) => void
  content: string
  onContentChange: (value: string) => void
  fieldErrors?: Record<string, string>
}

// Campos comunes de crear/editar: título, categorías (1–3) y contenido Markdown.
export function PostFields({
  title,
  onTitleChange,
  categoryIds,
  onCategoriesChange,
  content,
  onContentChange,
  fieldErrors = {},
}: PostFieldsProps) {
  return (
    <>
      <label className="block">
        <span className="field-label">Título</span>
        <input
          type="text"
          value={title}
          onChange={(e) => onTitleChange(e.target.value)}
          maxLength={150}
          className="input"
        />
        {fieldErrors.title && <p className="mt-1 text-sm text-red-700">{fieldErrors.title}</p>}
      </label>

      <div>
        <span className="field-label">Categorías (1 a 3)</span>
        <CategoryPicker selected={categoryIds} onChange={onCategoriesChange} />
        {fieldErrors.categoryIds && (
          <p className="mt-1 text-sm text-red-700">{fieldErrors.categoryIds}</p>
        )}
      </div>

      <div>
        <span className="field-label">Contenido (Markdown)</span>
        <MarkdownEditor value={content} onChange={onContentChange} />
        {fieldErrors.content && <p className="mt-1 text-sm text-red-700">{fieldErrors.content}</p>}
      </div>
    </>
  )
}
