import { api } from './client'
import type { ImageResponse } from './types'

// POST /posts/{postId}/images → 201 con la imagen subida. Multipart, campo "file", una por request.
// El backend valida por contenido real: 413 si supera el tamaño, 415 si el tipo no está permitido,
// 409 si se supera el máximo de imágenes por post.
export function uploadImage(postId: number, file: File): Promise<ImageResponse> {
  const formData = new FormData()
  formData.append('file', file)
  return api.postForm<ImageResponse>(`/posts/${postId}/images`, formData)
}

// DELETE /posts/{postId}/images/{imageId} → 204. Solo autor, solo si el post está ACTIVE.
export function deleteImage(postId: number, imageId: number): Promise<void> {
  return api.delete<void>(`/posts/${postId}/images/${imageId}`)
}
