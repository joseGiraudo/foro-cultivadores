import type { ContentStatus } from '../api/types'

const LABELS: Record<ContentStatus, string> = {
  ACTIVE: 'Activo',
  HIDDEN: 'Oculto',
  DELETED_BY_USER: 'Eliminado por el autor',
  DELETED_BY_MOD: 'Eliminado por moderación',
}

// Badge de estado de contenido. Se muestra solo para estados distintos de ACTIVE
// (lo normal es no ensuciar la UI cuando el contenido está activo).
export function StatusBadge({ status }: { status: ContentStatus }) {
  if (status === 'ACTIVE') return null
  return (
    <span className="rounded-full bg-sand-200 px-2 py-0.5 text-xs font-medium text-bark-700">
      {LABELS[status]}
    </span>
  )
}
