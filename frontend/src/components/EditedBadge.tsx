import { formatDate } from '../lib/format'

// Etiqueta sutil "editado" para posts y comentarios. No se muestra si nunca se editó
// (editedAt === null). El cambio de estado (ocultar/eliminar) no cuenta como edición.
export function EditedBadge({ editedAt }: { editedAt: string | null }) {
  if (!editedAt) return null
  return (
    <span className="italic text-gray-400" title={`Editado el ${formatDate(editedAt)}`}>
      · editado
    </span>
  )
}
