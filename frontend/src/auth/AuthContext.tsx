import { createContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { getMe, register as registerRequest } from '../api/auth'
import { ApiError, setAuthHeaderProvider } from '../api/client'
import type { UserResponse } from '../api/types'

// Clave donde se persiste el header Basic en sessionStorage (se pierde al cerrar la pestaña).
const STORAGE_KEY = 'foro.authHeader'

// Header Basic vigente. Vive a nivel de módulo para que el cliente HTTP (singleton) lo lea
// sin depender del árbol de React. Migrar a JWT = cambiar solo cómo se arma este valor.
let currentAuthHeader: string | null = sessionStorage.getItem(STORAGE_KEY)
setAuthHeaderProvider(() => currentAuthHeader)

function setAuthHeader(header: string | null): void {
  currentAuthHeader = header
  if (header) sessionStorage.setItem(STORAGE_KEY, header)
  else sessionStorage.removeItem(STORAGE_KEY)
}

// Codifica "usuario:contraseña" en Basic, seguro para UTF-8 (la contraseña puede tener acentos).
function basicHeader(username: string, password: string): string {
  const utf8 = new TextEncoder().encode(`${username}:${password}`)
  const binary = Array.from(utf8, (byte) => String.fromCharCode(byte)).join('')
  return `Basic ${btoa(binary)}`
}

export interface AuthContextValue {
  user: UserResponse | null
  // true mientras se revalida la sesión guardada al arrancar la app.
  initializing: boolean
  isAuthenticated: boolean
  isSuspended: boolean
  login: (username: string, password: string) => Promise<void>
  register: (username: string, email: string, password: string) => Promise<void>
  logout: () => void
  // Re-lee /users/me y actualiza el usuario en sesión (p. ej. tras editar la bio).
  refreshMe: () => Promise<void>
}

// eslint-disable-next-line react-refresh/only-export-components
export const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserResponse | null>(null)
  const [initializing, setInitializing] = useState<boolean>(currentAuthHeader !== null)

  // Al montar: si hay credenciales guardadas, revalidarlas contra /users/me.
  useEffect(() => {
    if (currentAuthHeader === null) return
    let cancelled = false
    getMe()
      .then((me) => {
        if (!cancelled) setUser(me)
      })
      .catch(() => {
        // 401 u otro error: la sesión guardada ya no sirve, la olvidamos.
        if (!cancelled) {
          setAuthHeader(null)
          setUser(null)
        }
      })
      .finally(() => {
        if (!cancelled) setInitializing(false)
      })
    return () => {
      cancelled = true
    }
  }, [])

  // Valida las credenciales contra /users/me. Deja la sesión activa si son válidas.
  async function login(username: string, password: string): Promise<void> {
    setAuthHeader(basicHeader(username, password))
    try {
      const me = await getMe()
      setUser(me)
    } catch (error) {
      setAuthHeader(null)
      setUser(null)
      if (error instanceof ApiError && error.status === 401) {
        throw new ApiError({ status: 401, detail: 'Usuario o contraseña incorrectos.' })
      }
      throw error
    }
  }

  // Registra y, si sale bien, hace login automático con esas credenciales.
  async function register(username: string, email: string, password: string): Promise<void> {
    await registerRequest({ username, email, password })
    await login(username, password)
  }

  function logout(): void {
    setAuthHeader(null)
    setUser(null)
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
      refreshMe,
    }),
    [user, initializing],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
