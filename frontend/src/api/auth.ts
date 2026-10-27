import { api } from './client'
import type { UserResponse } from './types'

export interface RegisterRequest {
  username: string
  email: string
  password: string
  acceptedTerms: boolean
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

// POST /auth/verify-email → confirma el email con el token del link. 204 ok; 400 si inválido/vencido.
export function verifyEmail(token: string): Promise<void> {
  return api.post<void>('/auth/verify-email', { token })
}

// POST /auth/resend-verification → reenvía el email de verificación. 204 (respuesta genérica).
export function resendVerification(email: string): Promise<void> {
  return api.post<void>('/auth/resend-verification', { email })
}

// POST /auth/forgot-password → inicia la recuperación. 204 (respuesta genérica, no revela si existe).
export function forgotPassword(email: string): Promise<void> {
  return api.post<void>('/auth/forgot-password', { email })
}

// POST /auth/reset-password → cambia la contraseña con el token del link. 204 ok; 400 si inválido/vencido.
export function resetPassword(token: string, password: string): Promise<void> {
  return api.post<void>('/auth/reset-password', { token, password })
}
