import { resolveImageUrl } from '../lib/format'

// Avatar del usuario. Si tiene foto (`src`, URL relativa del backend) la muestra; si no,
// deriva un color estable del nombre y muestra la inicial. Paleta cálida.
const COLORS = ['bg-leaf-500', 'bg-leaf-600', 'bg-leaf-700', 'bg-bark-500', 'bg-bark-600']

const SIZES = {
  xs: 'h-6 w-6 text-xs',
  sm: 'h-9 w-9 text-base',
  md: 'h-12 w-12 text-xl',
  lg: 'h-24 w-24 text-4xl',
} as const

function colorFor(name: string): string {
  let hash = 0
  for (let i = 0; i < name.length; i++) {
    hash = (hash * 31 + name.charCodeAt(i)) | 0
  }
  return COLORS[Math.abs(hash) % COLORS.length]
}

interface AvatarProps {
  name: string
  // URL relativa del backend (p. ej. /users/5/avatar?v=..). Si viene, se muestra la foto.
  src?: string | null
  size?: keyof typeof SIZES
  className?: string
}

export function Avatar({ name, src, size = 'md', className = '' }: AvatarProps) {
  const base = `shrink-0 rounded-full ${SIZES[size]} ${className}`

  if (src) {
    return (
      <img
        src={resolveImageUrl(src)}
        alt={name}
        loading="lazy"
        className={`${base} object-cover`}
      />
    )
  }

  const initial = name.trim().charAt(0).toUpperCase() || '?'
  return (
    <span
      aria-hidden
      className={`flex items-center justify-center font-display font-semibold text-cream-50 ${colorFor(name)} ${base}`}
    >
      {initial}
    </span>
  )
}
