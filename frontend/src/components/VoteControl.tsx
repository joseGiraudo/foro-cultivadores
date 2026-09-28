import type { VoteValue } from '../api/types'

interface VoteControlProps {
  score: number
  myVote: VoteValue | null
  // Vuelve a votar el mismo valor = quitar el voto (next === null).
  onVote: (next: VoteValue | null) => void
  disabled?: boolean
  // Motivo de deshabilitado (tooltip): sin sesión, contenido propio, suspendido, etc.
  title?: string
  pending?: boolean
  // Conteos desglosados: si se pasan, se muestra el número junto a cada botón
  // en vez del score neto en el medio (usado en el detalle del post).
  upvotes?: number
  downvotes?: number
}

// Control de voto up/down. Optimista: el estado lo maneja quien lo usa (hooks de voto).
// Modo score neto (por defecto) o modo desglosado (pasando upvotes/downvotes).
export function VoteControl({
  score,
  myVote,
  onVote,
  disabled,
  title,
  pending,
  upvotes,
  downvotes,
}: VoteControlProps) {
  const buttonBase =
    'flex h-7 items-center justify-center gap-1 rounded px-2 text-sm leading-none transition disabled:cursor-not-allowed'
  const split = upvotes !== undefined && downvotes !== undefined

  return (
    <div
      className="inline-flex items-center gap-1"
      title={title}
      aria-disabled={disabled}
    >
      <button
        type="button"
        disabled={disabled || pending}
        onClick={() => onVote(myVote === 'UPVOTE' ? null : 'UPVOTE')}
        aria-label="Votar positivo"
        aria-pressed={myVote === 'UPVOTE'}
        className={`${buttonBase} ${!split ? 'w-7 px-0' : ''} ${
          myVote === 'UPVOTE' ? 'bg-green-100 text-green-700' : 'text-gray-500 hover:bg-gray-100'
        } disabled:hover:bg-transparent disabled:opacity-50`}
      >
        <span>▲</span>
        {split && <span className="font-medium tabular-nums">{upvotes}</span>}
      </button>
      {!split && (
        <span className="min-w-6 text-center text-sm font-medium text-gray-800">{score}</span>
      )}
      <button
        type="button"
        disabled={disabled || pending}
        onClick={() => onVote(myVote === 'DOWNVOTE' ? null : 'DOWNVOTE')}
        aria-label="Votar negativo"
        aria-pressed={myVote === 'DOWNVOTE'}
        className={`${buttonBase} ${!split ? 'w-7 px-0' : ''} ${
          myVote === 'DOWNVOTE' ? 'bg-red-100 text-red-700' : 'text-gray-500 hover:bg-gray-100'
        } disabled:hover:bg-transparent disabled:opacity-50`}
      >
        <span>▼</span>
        {split && <span className="font-medium tabular-nums">{downvotes}</span>}
      </button>
    </div>
  )
}
