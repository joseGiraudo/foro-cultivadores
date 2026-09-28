import { useMutation, useQueryClient } from '@tanstack/react-query'
import { deleteImage, uploadImage } from '../api/images'
import {
  changePostStatus,
  createPost,
  updatePost,
  type CreatePostBody,
  type UpdatePostBody,
} from '../api/posts'

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

export function useUploadImage(postId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (file: File) => uploadImage(postId, file),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['post', postId] }),
  })
}

export function useDeleteImage(postId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (imageId: number) => deleteImage(postId, imageId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['post', postId] }),
  })
}
