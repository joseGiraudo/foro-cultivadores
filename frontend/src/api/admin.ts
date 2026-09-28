import { api, type QueryParams } from './client'
import type { Page, UserResponse } from './types'

export interface ListUsersParams {
  role?: string
  status?: string
  page?: number
}

// GET /admin/users → página de usuarios (incluye email). Filtros opcionales ?role= ?status=.
export function listUsers(params: ListUsersParams = {}): Promise<Page<UserResponse>> {
  return api.get<Page<UserResponse>>('/admin/users', params as QueryParams)
}

// PATCH /admin/users/{id}/role → cambia el rol. Un admin no puede cambiarse a sí mismo (409).
export function changeUserRole(id: number, role: string): Promise<UserResponse> {
  return api.patch<UserResponse>(`/admin/users/${id}/role`, { role })
}

// PATCH /admin/users/{id}/status → cambia el estado (ACTIVE/SUSPENDED). No sobre sí mismo (409).
export function changeUserStatus(id: number, status: string): Promise<UserResponse> {
  return api.patch<UserResponse>(`/admin/users/${id}/status`, { status })
}
