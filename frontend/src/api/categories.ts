import { api } from './client'
import type { Category } from './types'

// GET /categories → lista completa (no paginada) de categorías.
export function listCategories(): Promise<Category[]> {
  return api.get<Category[]>('/categories')
}

// POST /categories { name, description } → 201. Solo ADMIN. 409 si el nombre ya existe.
export function createCategory(name: string, description: string): Promise<Category> {
  return api.post<Category>('/categories', { name, description })
}
