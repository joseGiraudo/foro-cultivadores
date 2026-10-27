import { useState, type FormEvent } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { ApiError } from '../api/client'
import { uploadImage } from '../api/images'
import type { PostResponse } from '../api/types'
import { ImageUploader } from '../components/ImageUploader'
import { PostFields } from '../components/PostFields'
import { PostTypeBadge } from '../components/PostTypeBadge'
import { ErrorState, Loading } from '../components/states'
import { useAuth } from '../auth/useAuth'
import { useChangePostStatus, useUpdatePost } from '../hooks/usePostMutations'
import { usePost } from '../hooks/usePosts'
import { useToast } from '../toast/useToast'

export function EditPostPage() {
  const { id } = useParams<{ id: string }>()
  const postId = Number(id)
  const { user } = useAuth()
  const { data: post, isLoading, isError, error, refetch } = usePost(postId)

  if (isLoading) return <Loading />
  if (isError) return <ErrorState error={error} onRetry={refetch} />
  if (!post) return null

  if (user?.id !== post.author.id) {
    return <p className="text-ink-soft">No podés editar esta publicación.</p>
  }
  if (post.status !== 'ACTIVE') {
    return <p className="text-ink-soft">Esta publicación no está activa y no se puede editar.</p>
  }

  // key por id: el form inicializa su estado desde el post al montar (sin efecto de seeding); si se
  // navega a otro post, se remonta con los datos nuevos. El refetch (p. ej. tras subir una imagen en
  // consultas) NO cambia el id, así que no se remonta y no se pierden ediciones sin guardar.
  return <EditPostForm key={post.id} post={post} />
}

function EditPostForm({ post }: { post: PostResponse }) {
  const navigate = useNavigate()
  const update = useUpdatePost(post.id)
  const changeStatus = useChangePostStatus(post.id)
  const toast = useToast()

  const [title, setTitle] = useState(post.title)
  const [categoryIds, setCategoryIds] = useState<number[]>(post.categories.map((c) => c.id))
  const [content, setContent] = useState(post.content)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [formError, setFormError] = useState<string | null>(null)

  const isQuestion = post.postType === 'QUESTION'

  // Para el editor por bloques (artículos/guías): sube imágenes contra el post ya existente.
  async function handleUploadImage(file: File): Promise<string> {
    const image = await uploadImage(post.id, file)
    return image.url
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setFormError(null)

    const errors: Record<string, string> = {}
    if (title.trim().length < 5 || title.length > 150) {
      errors.title = 'El título debe tener entre 5 y 150 caracteres.'
    }
    if (content.trim().length < 1) errors.content = 'El contenido no puede estar vacío.'
    if (categoryIds.length < 1) errors.categoryIds = 'Elegí al menos una categoría.'
    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors)
      return
    }
    setFieldErrors({})
    try {
      await update.mutateAsync({ title, content, categoryIds })
      toast.success('Cambios guardados.')
    } catch (err) {
      if (err instanceof ApiError && err.status === 400 && err.fieldErrors) {
        setFieldErrors(err.fieldErrors)
      } else {
        setFormError(err instanceof ApiError ? err.message : 'No se pudieron guardar los cambios.')
      }
    }
  }

  async function handleDelete() {
    if (!window.confirm('¿Eliminar esta publicación? No se puede deshacer.')) return
    try {
      await changeStatus.mutateAsync('DELETED_BY_USER')
      toast.success('Publicación eliminada.')
      navigate('/', { replace: true })
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : 'No se pudo eliminar la publicación.')
    }
  }

  return (
    <div className="mx-auto max-w-2xl">
      <div className="mb-4 flex items-center gap-3">
        <h1 className="text-2xl font-semibold text-ink">Editar publicación</h1>
        <PostTypeBadge type={post.postType} />
      </div>

      <form onSubmit={handleSubmit} className="space-y-4" noValidate>
        <PostFields
          title={title}
          onTitleChange={setTitle}
          categoryIds={categoryIds}
          onCategoriesChange={setCategoryIds}
          content={content}
          onContentChange={setContent}
          fieldErrors={fieldErrors}
          contentMode={isQuestion ? 'plain' : 'blocks'}
          onUploadImage={isQuestion ? undefined : handleUploadImage}
        />

        {formError && <p className="text-sm text-red-700">{formError}</p>}

        <div className="flex flex-wrap gap-3">
          <button type="submit" disabled={update.isPending} className="btn-primary">
            {update.isPending ? 'Guardando…' : 'Guardar cambios'}
          </button>
          <button
            type="button"
            onClick={() => navigate(`/posts/${post.id}`)}
            className="btn-secondary"
          >
            Ver publicación
          </button>
          <button
            type="button"
            onClick={handleDelete}
            disabled={changeStatus.isPending}
            className="ml-auto rounded-lg border border-red-300 px-4 py-2 font-semibold text-red-700 transition hover:bg-red-50 disabled:opacity-50"
          >
            Eliminar
          </button>
        </div>
      </form>

      {/* Consultas: galería de fotos (orden de subida). Artículos/guías manejan sus imágenes
          dentro del editor por bloques, intercaladas con el texto. */}
      {isQuestion && (
        <section className="mt-8">
          <h2 className="mb-2 text-lg font-semibold text-ink">Imágenes</h2>
          <p className="mb-3 text-sm text-ink-faint">
            En las consultas las imágenes se muestran en galería, en el orden en que las subís.
          </p>
          <ImageUploader postId={post.id} images={post.images} />
        </section>
      )}
    </div>
  )
}
