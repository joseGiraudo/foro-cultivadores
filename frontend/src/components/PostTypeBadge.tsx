import type { PostType } from '../api/types'

const LABELS: Record<PostType, string> = {
  QUESTION: 'Consulta',
  ARTICLE: 'Artículo',
  GUIDE: 'Guía',
}

const STYLES: Record<PostType, string> = {
  QUESTION: 'bg-amber-100 text-amber-800',
  ARTICLE: 'bg-sky-100 text-sky-800',
  GUIDE: 'bg-emerald-100 text-emerald-800',
}

export function PostTypeBadge({ type }: { type: PostType }) {
  return (
    <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${STYLES[type]}`}>
      {LABELS[type]}
    </span>
  )
}
