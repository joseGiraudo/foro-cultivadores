import { BlockEditor } from './BlockEditor'
import { CategoryPicker } from './CategoryPicker'

interface PostFieldsProps {
  title: string
  onTitleChange: (value: string) => void
  categoryIds: number[]
  onCategoriesChange: (ids: number[]) => void
  content: string
  onContentChange: (value: string) => void
  fieldErrors?: Record<string, string>
  // 'blocks' (artículos/guías, editor visual por bloques) o 'plain' (consultas, textarea simple).
  contentMode?: 'blocks' | 'plain'
  // Subida de imágenes del editor por bloques (solo donde hay postId, p. ej. al editar).
  onUploadImage?: (file: File) => Promise<string>
}

// Campos comunes de crear/editar: título, categorías (1–4) y contenido.
export function PostFields({
  title,
  onTitleChange,
  categoryIds,
  onCategoriesChange,
  content,
  onContentChange,
  fieldErrors = {},
  contentMode = 'blocks',
  onUploadImage,
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
        <span className="field-label">Categorías (1 a 4)</span>
        <CategoryPicker selected={categoryIds} onChange={onCategoriesChange} />
        {fieldErrors.categoryIds && (
          <p className="mt-1 text-sm text-red-700">{fieldErrors.categoryIds}</p>
        )}
      </div>

      <div>
        {contentMode === 'plain' ? (
          <label className="block">
            <span className="field-label">Contenido</span>
            <textarea
              value={content}
              onChange={(e) => onContentChange(e.target.value)}
              rows={6}
              maxLength={20000}
              placeholder="Contá tu consulta: qué le pasa a tu planta, qué venís haciendo, etc."
              className="input min-h-[8rem] resize-y"
            />
          </label>
        ) : (
          <>
            <span className="field-label">Contenido</span>
            <BlockEditor initialValue={content} onChange={onContentChange} onUploadImage={onUploadImage} />
          </>
        )}
        {fieldErrors.content && <p className="mt-1 text-sm text-red-700">{fieldErrors.content}</p>}
      </div>
    </>
  )
}
