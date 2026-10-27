import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { ApiError } from '../api/client'
import type { PostType } from '../api/types'
import { NewPostImagePicker } from '../components/NewPostImagePicker'
import { PostFields } from '../components/PostFields'
import { useCreatableTypes } from '../hooks/usePosts'
import { useCreatePost, useCreatePostWithImages } from '../hooks/usePostMutations'
import { useToast } from '../toast/useToast'

const TYPE_OPTIONS: { value: PostType; label: string; hint: string }[] = [
  { value: 'QUESTION', label: 'Consulta', hint: 'Una pregunta a la comunidad.' },
  { value: 'ARTICLE', label: 'Artículo', hint: 'Una nota o reseña para compartir con la comunidad.' },
  { value: 'GUIDE', label: 'Guía', hint: 'Un instructivo paso a paso.' },
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
  const createWithImages = useCreatePostWithImages()
  const toast = useToast()
  const { data: creatableTypes } = useCreatableTypes()

  const [title, setTitle] = useState('')
  const [postType, setPostType] = useState<PostType>('QUESTION')
  const [categoryIds, setCategoryIds] = useState<number[]>([])
  const [content, setContent] = useState('')
  const [images, setImages] = useState<File[]>([])
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [formError, setFormError] = useState<string | null>(null)

  // Tipos que el backend deja crear a este usuario (QUESTION siempre; ARTICLE/GUIDE solo con rol).
  // Hasta que carguen, se asume solo Consulta (opción universal) para no mostrar de más.
  const allowedTypes = creatableTypes ?? ['QUESTION']
  const typeOptions = TYPE_OPTIONS.filter((option) => allowedTypes.includes(option.value))
  const showTypeSelect = typeOptions.length > 1

  const isQuestion = postType === 'QUESTION'
  const selectedHint = TYPE_OPTIONS.find((option) => option.value === postType)?.hint
  const pending = create.isPending || createWithImages.isPending

  function mapError(err: unknown) {
    if (err instanceof ApiError) {
      if (err.status === 403) {
        setFormError('Tu rol no alcanza para crear este tipo de publicación.')
      } else if (err.status === 400 && err.fieldErrors) {
        setFieldErrors(err.fieldErrors)
      } else if (err.status === 413) {
        setFormError('Alguna imagen supera el tamaño máximo (5 MB).')
      } else if (err.status === 415) {
        setFormError('Alguna imagen tiene un tipo no permitido (usá JPEG, PNG o WebP).')
      } else {
        setFormError(err.message)
      }
    } else {
      setFormError('No se pudo crear la publicación.')
    }
  }

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
      if (isQuestion) {
        // Consulta: creación en un solo paso (post + fotos en la misma request) → al detalle.
        const post = await createWithImages.mutateAsync({
          body: { title, content, postType, categoryIds },
          files: images,
        })
        toast.success('Consulta publicada.')
        navigate(`/posts/${post.id}`, { replace: true })
      } else {
        // Artículo/Guía: se crea y se va a la edición para armar el contenido con imágenes.
        const post = await create.mutateAsync({ title, content, postType, categoryIds })
        toast.success('Publicación creada.')
        navigate(`/posts/${post.id}/edit`, { replace: true })
      }
    } catch (err) {
      mapError(err)
    }
  }

  return (
    <div className="mx-auto max-w-2xl">
      <h1 className="mb-4 text-2xl font-semibold text-ink">Nueva publicación</h1>
      <form onSubmit={handleSubmit} className="space-y-4" noValidate>
        {showTypeSelect && (
          <div>
            <span className="field-label">Tipo</span>
            <select
              value={postType}
              onChange={(e) => setPostType(e.target.value as PostType)}
              className="select"
            >
              {typeOptions.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
            {selectedHint && <p className="mt-1 text-xs text-ink-faint">{selectedHint}</p>}
          </div>
        )}

        {!isQuestion && (
          <div className="rounded-lg border border-leaf-300 bg-leaf-100 p-3 text-sm text-ink-soft">
            <strong className="font-semibold text-leaf-700">Cómo conviene armarla:</strong> primero
            escribí el texto y creá la publicación. Después, al editarla, vas a poder subir las
            imágenes y ubicarlas entre los párrafos.
          </div>
        )}

        <PostFields
          title={title}
          onTitleChange={setTitle}
          categoryIds={categoryIds}
          onCategoriesChange={setCategoryIds}
          content={content}
          onContentChange={setContent}
          fieldErrors={fieldErrors}
          contentMode={isQuestion ? 'plain' : 'blocks'}
        />

        {isQuestion && <NewPostImagePicker files={images} onChange={setImages} />}

        {formError && <p className="text-sm text-red-700">{formError}</p>}

        <div className="flex gap-3">
          <button type="submit" disabled={pending} className="btn-primary">
            {pending ? 'Publicando…' : isQuestion ? 'Publicar consulta' : 'Crear y agregar imágenes'}
          </button>
          <button type="button" onClick={() => navigate('/')} className="btn-secondary">
            Cancelar
          </button>
        </div>
      </form>
    </div>
  )
}
