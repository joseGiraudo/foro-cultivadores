import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { ApiError } from '../api/client'
import type { PostType } from '../api/types'
import { PostFields } from '../components/PostFields'
import { useCreatePost } from '../hooks/usePostMutations'

const TYPE_OPTIONS: { value: PostType; label: string; hint: string }[] = [
  { value: 'QUESTION', label: 'Consulta', hint: 'Una pregunta a la comunidad.' },
  { value: 'ARTICLE', label: 'Artículo', hint: 'Requiere ser Grow (CONTRIBUTOR) o superior.' },
  { value: 'GUIDE', label: 'Guía', hint: 'Requiere ser Grow (CONTRIBUTOR) o superior.' },
]

function validate(title: string, content: string, categoryIds: number[]): Record<string, string> {
  const errors: Record<string, string> = {}
  if (title.trim().length < 5 || title.length > 150) {
    errors.title = 'El título debe tener entre 5 y 150 caracteres.'
  }
  if (content.trim().length < 1) errors.content = 'El contenido no puede estar vacío.'
  if (categoryIds.length < 1) errors.categoryIds = 'Elegí al menos una categoría.'
  return errors
}

export function CreatePostPage() {
  const navigate = useNavigate()
  const create = useCreatePost()

  const [title, setTitle] = useState('')
  const [postType, setPostType] = useState<PostType>('QUESTION')
  const [categoryIds, setCategoryIds] = useState<number[]>([])
  const [content, setContent] = useState('')
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [formError, setFormError] = useState<string | null>(null)

  const selectedHint = TYPE_OPTIONS.find((option) => option.value === postType)?.hint

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setFormError(null)

    const clientErrors = validate(title, content, categoryIds)
    if (Object.keys(clientErrors).length > 0) {
      setFieldErrors(clientErrors)
      return
    }
    setFieldErrors({})
    try {
      const post = await create.mutateAsync({ title, content, postType, categoryIds })
      // Flujo en dos pasos: tras crear, se va a la edición para poder subir imágenes.
      navigate(`/posts/${post.id}/edit`, { replace: true })
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.status === 403) {
          setFormError('Tu rol no alcanza para crear este tipo de publicación.')
        } else if (err.status === 400 && err.fieldErrors) {
          setFieldErrors(err.fieldErrors)
        } else {
          setFormError(err.message)
        }
      } else {
        setFormError('No se pudo crear la publicación.')
      }
    }
  }

  return (
    <div className="mx-auto max-w-2xl">
      <h1 className="mb-4 text-2xl font-semibold text-ink">Nueva publicación</h1>
      <form onSubmit={handleSubmit} className="space-y-4" noValidate>
        <div>
          <span className="field-label">Tipo</span>
          <select
            value={postType}
            onChange={(e) => setPostType(e.target.value as PostType)}
            className="select"
          >
            {TYPE_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
          {selectedHint && <p className="mt-1 text-xs text-ink-faint">{selectedHint}</p>}
        </div>

        <PostFields
          title={title}
          onTitleChange={setTitle}
          categoryIds={categoryIds}
          onCategoriesChange={setCategoryIds}
          content={content}
          onContentChange={setContent}
          fieldErrors={fieldErrors}
        />

        {formError && <p className="text-sm text-red-700">{formError}</p>}

        <div className="flex gap-3">
          <button type="submit" disabled={create.isPending} className="btn-primary">
            {create.isPending ? 'Creando…' : 'Crear publicación'}
          </button>
          <button type="button" onClick={() => navigate('/')} className="btn-secondary">
            Cancelar
          </button>
        </div>
      </form>

      <p className="mt-3 text-xs text-ink-faint">
        Después de crear la publicación vas a poder subir imágenes.
      </p>
    </div>
  )
}
