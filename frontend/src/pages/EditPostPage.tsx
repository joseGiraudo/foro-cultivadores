import { useEffect, useRef, useState, type FormEvent } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { ApiError } from '../api/client'
import type { ImageResponse } from '../api/types'
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
  const navigate = useNavigate()
  const { user } = useAuth()

  const { data: post, isLoading, isError, error, refetch } = usePost(postId)
  const update = useUpdatePost(postId)
  const changeStatus = useChangePostStatus(postId)
  const toast = useToast()

  const [title, setTitle] = useState('')
  const [categoryIds, setCategoryIds] = useState<number[]>([])
  const [content, setContent] = useState('')
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [formError, setFormError] = useState<string | null>(null)

  // Sembrar el formulario una sola vez por post. Importante: subir/borrar imágenes invalida
  // ['post', id] y refetchea; sin este guard, se perderían las ediciones sin guardar.
  const seededId = useRef<number | null>(null)
  useEffect(() => {
    if (post && seededId.current !== post.id) {
      setTitle(post.title)
      setCategoryIds(post.categories.map((category) => category.id))
      setContent(post.content)
      seededId.current = post.id
    }
  }, [post])

  if (isLoading) return <Loading />
  if (isError) return <ErrorState error={error} onRetry={refetch} />
  if (!post) return null

  const isAuthor = user?.id === post.author.id
  if (!isAuthor) {
    return <p className="text-ink-soft">No podés editar esta publicación.</p>
  }
  if (post.status !== 'ACTIVE') {
    return <p className="text-ink-soft">Esta publicación no está activa y no se puede editar.</p>
  }

  // En artículos/guías las imágenes se referencian en el Markdown; en consultas van en galería.
  const canInsertImages = post.postType !== 'QUESTION'

  function insertReference(image: ImageResponse) {
    setContent((current) => `${current.trimEnd()}\n\n![](${image.url})\n`)
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

      <section className="mt-8">
        <h2 className="mb-2 text-lg font-semibold text-ink">Imágenes</h2>
        <p className="mb-3 text-sm text-ink-faint">
          {canInsertImages
            ? 'Subí imágenes y usá "Insertar en el texto" para referenciarlas en el contenido. Acordate de guardar los cambios.'
            : 'En las consultas las imágenes se muestran en galería, en el orden en que las subís.'}
        </p>
        <ImageUploader
          postId={post.id}
          images={post.images}
          onInsertReference={canInsertImages ? insertReference : undefined}
        />
      </section>
    </div>
  )
}
