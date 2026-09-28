import type { PostResponse } from '../api/types'
import { useAuth } from '../auth/useAuth'
import { usePostVote } from '../hooks/useVote'
import { VoteControl } from './VoteControl'

// Conecta el VoteControl de un post con el hook de voto y el estado de sesión.
// La UI deshabilita los casos que el backend igual rechazaría (comodidad, no seguridad):
// sin sesión, contenido propio (auto-voto → 403) o cuenta suspendida.
export function PostVoteControl({ post }: { post: PostResponse }) {
  const { user, isSuspended } = useAuth()
  const vote = usePostVote(post.id)

  const isOwn = user?.id === post.author.id
  const disabled = !user || isOwn || isSuspended
  const title = !user
    ? 'Ingresá para votar'
    : isSuspended
      ? 'Tu cuenta está suspendida'
      : isOwn
        ? 'No podés votar tu propio contenido'
        : undefined

  return (
    <VoteControl
      score={post.score}
      myVote={post.myVote}
      upvotes={post.upvotes}
      downvotes={post.downvotes}
      onVote={(next) => vote.mutate(next)}
      disabled={disabled}
      title={title}
      pending={vote.isPending}
    />
  )
}
