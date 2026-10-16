import { useMutation, useQueryClient } from '@tanstack/react-query'
import { deleteImage, uploadImage } from '../api/images'
import {
  changePostStatus,
  createPost,
  savePost,
  unsavePost,
  updatePost,
  type CreatePostBody,
  type UpdatePostBody,
} from '../api/posts'
import type { PostResponse } from '../api/types'

export function useCreatePost() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: CreatePostBody) => createPost(body),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['posts'] }),
  })
}

export function useUpdatePost(postId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: UpdatePostBody) => updatePost(postId, body),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['post', postId] })
      qc.invalidateQueries({ queryKey: ['posts'] })
    },
  })
}

export function useChangePostStatus(postId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (status: string) => changePostStatus(postId, status),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['post', postId] })
      qc.invalidateQueries({ queryKey: ['posts'] })
    },
  })
}

// Guardar/quitar de guardados con actualización optimista sobre el detalle en cache.
export function useSavePost(postId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (next: boolean) => (next ? savePost(postId) : unsavePost(postId)),
    onMutate: async (next) => {
      await qc.cancelQueries({ queryKey: ['post', postId] })
      const prev = qc.getQueryData<PostResponse>(['post', postId])
      if (prev) {
        qc.setQueryData<PostResponse>(['post', postId], {
          ...prev,
          saved: next,
          saveCount: prev.saveCount + (next ? 1 : -1),
        })
      }
      return { prev }
    },
    onError: (_error, _next, context) => {
      if (context?.prev) qc.setQueryData(['post', postId], context.prev)
    },
    onSettled: () => {
      qc.invalidateQueries({ queryKey: ['post', postId] })
      qc.invalidateQueries({ queryKey: ['savedPosts'] })
      qc.invalidateQueries({ queryKey: ['posts'] })
    },
  })
}

export interface UploadImagesResult {
  uploaded: number
  failed: number
  firstError: unknown | null
}

// Sube varias imágenes en secuencia (el backend acepta una por request). Secuencial y no en
// paralelo para que el límite por post se valide sin carreras (evita 409 por conteo concurrente).
// No corta en el primer error: intenta todas y reporta cuántas fallaron.
export function useUploadImages(postId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (files: File[]): Promise<UploadImagesResult> => {
      let uploaded = 0
      let failed = 0
      let firstError: unknown = null
      for (const file of files) {
        try {
          await uploadImage(postId, file)
          uploaded += 1
        } catch (err) {
          failed += 1
          if (firstError === null) firstError = err
        }
      }
      return { uploaded, failed, firstError }
    },
    onSettled: () => qc.invalidateQueries({ queryKey: ['post', postId] }),
  })
}

export function useDeleteImage(postId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (imageId: number) => deleteImage(postId, imageId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['post', postId] }),
  })
}
