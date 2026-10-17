import { ApiError } from '../api/client'
import type { UserResponse } from '../api/types'
import { useAuth } from '../auth/useAuth'
import { useFollow } from '../hooks/useUsers'
import { useToast } from '../toast/useToast'

// Botón seguir/dejar de seguir. No se muestra en el perfil propio.
// La UI no hardcodea follow-target-min-role: intenta seguir y maneja el 400.
export function FollowButton({ profile }: { profile: UserResponse }) {
  const { user, isSuspended } = useAuth()
  const follow = useFollow(profile.id, profile.username)
  const toast = useToast()

  // Sin sesión o es el propio perfil: no mostramos el botón.
  if (!user || user.id === profile.id) return null

  const following = profile.followedByMe === true

  function handleClick() {
    follow.mutate(!following, {
      onError: (err) => {
        if (err instanceof ApiError && err.status === 400) {
          toast.error('Solo podés seguir a Grows (o roles superiores).')
        } else {
          toast.error(err instanceof ApiError ? err.message : 'No se pudo completar la acción.')
        }
      },
    })
  }

  return (
    <button
      type="button"
      onClick={handleClick}
      disabled={follow.isPending || isSuspended}
      title={isSuspended ? 'Tu cuenta está suspendida' : undefined}
      className={`rounded-lg px-4 py-1.5 text-sm font-semibold transition disabled:opacity-50 ${
        following
          ? 'border border-sand-200 text-ink-soft hover:bg-leaf-100 hover:text-leaf-700'
          : 'bg-leaf-600 text-cream-50 shadow-soft hover:bg-leaf-700'
      }`}
    >
      {following ? 'Siguiendo' : 'Seguir'}
    </button>
  )
}
