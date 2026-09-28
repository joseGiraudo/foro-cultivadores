import { api } from './client'
import type { Page, UserResponse } from './types'

// GET /users/{id} → perfil público (sin email).
export function getUser(id: number): Promise<UserResponse> {
  return api.get<UserResponse>(`/users/${id}`)
}

// PATCH /users/me { bio } → perfil propio actualizado (incluye email).
export function updateMe(bio: string): Promise<UserResponse> {
  return api.patch<UserResponse>('/users/me', { bio })
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
