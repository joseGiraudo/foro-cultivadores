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

// POST /auth/login → abre sesión (cookie httpOnly) y devuelve el perfil propio.
// 401 si usuario/contraseña incorrectos.
export function login(body: { username: string; password: string }): Promise<UserResponse> {
  return api.post<UserResponse>('/auth/login', body)
}

// POST /auth/logout → cierra la sesión (204). Idempotente.
export function logout(): Promise<void> {
  return api.post<void>('/auth/logout')
}

// GET /users/me → perfil propio (incluye email). 200 = sesión válida; 401 = sin sesión.
export function getMe(): Promise<UserResponse> {
  return api.get<UserResponse>('/users/me')
}
