import { api } from './client'
import type { Page, UpdateMeInput, UserResponse } from './types'

// GET /users/{id} → perfil público (sin email).
export function getUser(id: number): Promise<UserResponse> {
  return api.get<UserResponse>(`/users/${id}`)
}

// GET /users/by-username/{username} → perfil público por nombre (sin email).
// Se usa para las URLs de perfil, que exponen el username en vez del id.
export function getUserByUsername(username: string): Promise<UserResponse> {
  return api.get<UserResponse>(`/users/by-username/${encodeURIComponent(username)}`)
}

// PATCH /users/me → perfil propio actualizado (incluye email). Manda bio y, para roles
// que lo permitan, los datos de comercio (el backend responde 403 si el rol no alcanza).
export function updateMe(input: UpdateMeInput): Promise<UserResponse> {
  return api.patch<UserResponse>('/users/me', input)
}

// POST/DELETE /users/{id}/follow → 204 (idempotentes).
// Seguir a alguien con rol menor a follow-target-min-role (CONTRIBUTOR por config) → 400.
// Seguirse a uno mismo → 400.
export function followUser(id: number): Promise<void> {
  return api.post<void>(`/users/${id}/follow`)
}

export function unfollowUser(id: number): Promise<void> {
  return api.delete<void>(`/users/${id}/follow`)
}

export function getFollowers(id: number, page = 0): Promise<Page<UserResponse>> {
  return api.get<Page<UserResponse>>(`/users/${id}/followers`, { page })
}

export function getFollowing(id: number, page = 0): Promise<Page<UserResponse>> {
  return api.get<Page<UserResponse>>(`/users/${id}/following`, { page })
}

// Imágenes del usuario (multipart campo "file"). Devuelven el perfil propio actualizado.
// La portada requiere rol suficiente (el backend responde 403 si no).
export function uploadAvatar(file: File): Promise<UserResponse> {
  const formData = new FormData()
  formData.append('file', file)
  return api.postForm<UserResponse>('/users/me/avatar', formData)
}

export function deleteAvatar(): Promise<UserResponse> {
  return api.delete<UserResponse>('/users/me/avatar')
}

export function uploadCover(file: File): Promise<UserResponse> {
  const formData = new FormData()
  formData.append('file', file)
  return api.postForm<UserResponse>('/users/me/cover', formData)
}

export function deleteCover(): Promise<UserResponse> {
  return api.delete<UserResponse>('/users/me/cover')
}
