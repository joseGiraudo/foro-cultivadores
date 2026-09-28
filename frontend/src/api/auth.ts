import { api } from './client'
import type { UserResponse } from './types'

export interface RegisterRequest {
  username: string
  email: string
  password: string
}

// POST /auth/register → 201 con el perfil creado (role=USER, status=ACTIVE).
// Puede devolver 409 si el username o el email ya están en uso.
export function register(body: RegisterRequest): Promise<UserResponse> {
  return api.post<UserResponse>('/auth/register', body)
}

// GET /users/me → perfil propio (incluye email). Sirve para validar credenciales Basic:
// 200 = credenciales válidas; 401 = inválidas.
export function getMe(): Promise<UserResponse> {
  return api.get<UserResponse>('/users/me')
}
