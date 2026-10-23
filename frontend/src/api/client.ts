// Cliente HTTP único de la app. Toda llamada a la API pasa por acá.
//
// Responsabilidades:
//  - Antepone /api a las rutas (el proxy de Vite reescribe /api → backend).
//  - Inyecta el header Authorization: Basic ... si hay sesión (se enchufa en el Hito 2).
//  - Traduce los errores ProblemDetail (RFC 9457) a una ApiError tipada.
//
// La autenticación queda AISLADA acá: migrar de Basic a JWT es cambiar solo este archivo.

import type { ProblemDetail } from './types'

const API_PREFIX = '/api'

// Query params admitidos: se ignoran los undefined/null al construir la URL.
// Un array se serializa repitiendo la clave (key=a&key=b) → Spring lo bindea a List<>.
export type QueryParams = Record<
  string,
  string | number | boolean | undefined | null | (string | number)[]
>

// Proveedor del header de autorización. En el Hito 2, AuthContext registra acá la función
// que devuelve el header Basic (o null si no hay sesión). Mientras tanto, no hay auth.
let authHeaderProvider: () => string | null = () => null

export function setAuthHeaderProvider(provider: () => string | null): void {
  authHeaderProvider = provider
}

// Error de API con la info del ProblemDetail para que la UI decida cómo mostrarlo.
export class ApiError extends Error {
  readonly status: number
  readonly detail?: string
  readonly fieldErrors?: Record<string, string>

  constructor(problem: ProblemDetail) {
    super(problem.detail ?? problem.title ?? `Error ${problem.status}`)
    this.name = 'ApiError'
    this.status = problem.status
    this.detail = problem.detail
    this.fieldErrors = problem.errors
  }
}

interface RequestOptions {
  method?: string
  // Body JSON serializable; para multipart usar `formData`.
  body?: unknown
  formData?: FormData
  // Query params (se ignoran los undefined/null).
  params?: QueryParams
}

function buildUrl(path: string, params?: RequestOptions['params']): string {
  const url = `${API_PREFIX}${path}`
  if (!params) return url
  const search = new URLSearchParams()
  for (const [key, value] of Object.entries(params)) {
    if (value === undefined || value === null) continue
    if (Array.isArray(value)) {
      for (const item of value) search.append(key, String(item))
    } else {
      search.append(key, String(value))
    }
  }
  const qs = search.toString()
  return qs ? `${url}?${qs}` : url
}

async function toApiError(response: Response): Promise<ApiError> {
  let problem: ProblemDetail = { status: response.status }
  try {
    const data = (await response.json()) as ProblemDetail
    problem = { ...data, status: data.status ?? response.status }
  } catch {
    // El backend no devolvió JSON (o cuerpo vacío): usamos solo el status.
  }
  return new ApiError(problem)
}

async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = 'GET', body, formData, params } = options
  const headers: Record<string, string> = {}

  const authHeader = authHeaderProvider()
  if (authHeader) headers['Authorization'] = authHeader

  let payload: BodyInit | undefined
  if (formData) {
    payload = formData // el navegador setea el Content-Type con el boundary
  } else if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
    payload = JSON.stringify(body)
  }

  const response = await fetch(buildUrl(path, params), { method, headers, body: payload })

  if (!response.ok) throw await toApiError(response)

  // 204 No Content (o cuerpo vacío): no hay nada que parsear.
  if (response.status === 204 || response.headers.get('content-length') === '0') {
    return undefined as T
  }
  const contentType = response.headers.get('content-type') ?? ''
  if (!contentType.includes('application/json')) return undefined as T
  return (await response.json()) as T
}

export const api = {
  get: <T>(path: string, params?: RequestOptions['params']) => request<T>(path, { params }),
  post: <T>(path: string, body?: unknown) => request<T>(path, { method: 'POST', body }),
  put: <T>(path: string, body?: unknown) => request<T>(path, { method: 'PUT', body }),
  patch: <T>(path: string, body?: unknown) => request<T>(path, { method: 'PATCH', body }),
  delete: <T>(path: string) => request<T>(path, { method: 'DELETE' }),
  postForm: <T>(path: string, formData: FormData) =>
    request<T>(path, { method: 'POST', formData }),
}
