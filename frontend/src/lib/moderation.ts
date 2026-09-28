import type { ContentStatus } from '../api/types'

export interface ModerationTarget {
  label: string
  status: ContentStatus
}

// Transiciones de moderación disponibles según el estado actual (iguales para posts y comentarios,
// ver mvp-spec §6). Los estados DELETED_* son terminales (sin acciones).
export function moderationTargets(status: ContentStatus): ModerationTarget[] {
  switch (status) {
    case 'ACTIVE':
      return [
        { label: 'Ocultar', status: 'HIDDEN' },
        { label: 'Eliminar (mod)', status: 'DELETED_BY_MOD' },
      ]
    case 'HIDDEN':
      return [
        { label: 'Restaurar', status: 'ACTIVE' },
        { label: 'Eliminar (mod)', status: 'DELETED_BY_MOD' },
      ]
    default:
      return []
  }
}
