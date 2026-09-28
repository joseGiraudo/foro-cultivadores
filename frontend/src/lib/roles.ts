import type { Role } from '../api/types'

// Jerarquía de roles del backend: USER < CONTRIBUTOR < MODERATOR < ADMIN.
const ORDER: Record<Role, number> = {
  USER: 0,
  CONTRIBUTOR: 1,
  MODERATOR: 2,
  ADMIN: 3,
}

// Ídem `role.isAtLeast(min)` del backend. Solo para ordenar la navegación/acciones (comodidad);
// la autorización real la hace el backend.
export function isAtLeast(role: Role | undefined, min: Role): boolean {
  if (!role) return false
  return ORDER[role] >= ORDER[min]
}
