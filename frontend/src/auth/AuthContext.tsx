import { createContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import {
  getMe,
  login as loginRequest,
  logout as logoutRequest,
  register as registerRequest,
} from '../api/auth'
import { deleteMe } from '../api/users'
import { ApiError } from '../api/client'
import type { UserResponse } from '../api/types'

export interface AuthContextValue {
  user: UserResponse | null
  // true mientras se revalida la sesión (cookie) al arrancar la app.
  initializing: boolean
  isAuthenticated: boolean
  isSuspended: boolean
  login: (username: string, password: string) => Promise<void>
  register: (username: string, email: string, password: string, acceptedTerms: boolean) => Promise<void>
  logout: () => Promise<void>
  // Baja de cuenta (anonimización). Cierra la sesión local al terminar.
  deleteAccount: () => Promise<void>
  // Re-lee /users/me y actualiza el usuario en sesión (p. ej. tras editar la bio).
  refreshMe: () => Promise<void>
}

// eslint-disable-next-line react-refresh/only-export-components
export const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserResponse | null>(null)
  const [initializing, setInitializing] = useState<boolean>(true)

  // Al montar: intentar restaurar la sesión desde la cookie (GET /users/me).
  useEffect(() => {
    let cancelled = false
    getMe()
      .then((me) => {
        if (!cancelled) setUser(me)
      })
      .catch(() => {
        // 401 (sin sesión) u otro error: queda anónimo.
        if (!cancelled) setUser(null)
      })
      .finally(() => {
        if (!cancelled) setInitializing(false)
      })
    return () => {
      cancelled = true
    }
  }, [])

  // Abre sesión contra el backend (la cookie la setea el server). Devuelve el perfil propio.
  async function login(username: string, password: string): Promise<void> {
    try {
      const me = await loginRequest({ username, password })
      setUser(me)
    } catch (error) {
      setUser(null)
      if (error instanceof ApiError && error.status === 401) {
        throw new ApiError({ status: 401, detail: 'Usuario o contraseña incorrectos.' })
      }
      throw error
    }
  }

  // Registra la cuenta. NO abre sesión: el usuario debe verificar su email antes de poder ingresar.
  async function register(
    username: string,
    email: string,
    password: string,
    acceptedTerms: boolean,
  ): Promise<void> {
    await registerRequest({ username, email, password, acceptedTerms })
  }

  async function logout(): Promise<void> {
    try {
      await logoutRequest()
    } finally {
      // Pase lo que pase en el server, localmente olvidamos la sesión.
      setUser(null)
    }
  }

  async function deleteAccount(): Promise<void> {
    await deleteMe()
    setUser(null) // la baja también invalida la sesión en el server
  }

  async function refreshMe(): Promise<void> {
    const me = await getMe()
    setUser(me)
  }

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      initializing,
      isAuthenticated: user !== null,
      isSuspended: user?.status === 'SUSPENDED',
      login,
      register,
      logout,
      deleteAccount,
      refreshMe,
    }),
    [user, initializing],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
