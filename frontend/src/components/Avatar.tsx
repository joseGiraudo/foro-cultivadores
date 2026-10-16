// Avatar con iniciales. No hay imágenes de usuario en el backend todavía, así que
// derivamos un color estable del nombre y mostramos la inicial. Paleta cálida.
const COLORS = ['bg-leaf-500', 'bg-leaf-600', 'bg-leaf-700', 'bg-bark-500', 'bg-bark-600']

const SIZES = {
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
  size?: keyof typeof SIZES
  className?: string
}

export function Avatar({ name, size = 'md', className = '' }: AvatarProps) {
  const initial = name.trim().charAt(0).toUpperCase() || '?'
  return (
    <span
      aria-hidden
      className={`flex shrink-0 items-center justify-center rounded-full font-display font-semibold text-cream-50 ${colorFor(name)} ${SIZES[size]} ${className}`}
    >
      {initial}
    </span>
  )
}
