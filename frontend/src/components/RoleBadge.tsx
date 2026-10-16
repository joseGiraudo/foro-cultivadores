import type { Role } from '../api/types'

const LABELS: Record<Role, string> = {
  USER: 'Usuario',
  CONTRIBUTOR: 'Grow',
  MODERATOR: 'Moderador',
  ADMIN: 'Admin',
}

const STYLES: Record<Role, string> = {
  USER: 'bg-sand-200 text-bark-700',
  CONTRIBUTOR: 'bg-leaf-100 text-leaf-700',
  MODERATOR: 'bg-blue-100 text-blue-800',
  ADMIN: 'bg-purple-100 text-purple-800',
}

export function RoleBadge({ role }: { role: Role }) {
  return (
    <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${STYLES[role]}`}>
      {LABELS[role]}
    </span>
  )
}
