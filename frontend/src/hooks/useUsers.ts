import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  followUser,
  getFollowers,
  getFollowing,
  getUser,
  getUserByUsername,
  unfollowUser,
  updateMe,
} from '../api/users'
import type { UpdateMeInput, UserResponse } from '../api/types'
import { useAuth } from '../auth/useAuth'

export function useUser(id: number) {
  return useQuery({
    queryKey: ['user', id],
    queryFn: () => getUser(id),
  })
}

// Perfil por username (para las rutas /users/:username). La cache se indexa por
// username; el id real viaja dentro de la respuesta para las llamadas internas.
export function useUserByUsername(username: string) {
  return useQuery({
    queryKey: ['user', 'by-username', username],
    queryFn: () => getUserByUsername(username),
    enabled: username.length > 0,
  })
}

export function useUpdateMe() {
  const { refreshMe } = useAuth()
  return useMutation({
    mutationFn: (input: UpdateMeInput) => updateMe(input),
    onSuccess: () => refreshMe(),
  })
}

// Seguir/dejar de seguir con actualización optimista sobre el perfil en cache.
// El perfil se muestra cacheado por username (ruta /users/:username), así que el
// optimismo opera sobre esa clave; igual invalidamos todo al terminar.
export function useFollow(userId: number, username?: string) {
  const qc = useQueryClient()
  const profileKey = username ? ['user', 'by-username', username] : ['user', userId]
  return useMutation({
    mutationFn: (next: boolean) => (next ? followUser(userId) : unfollowUser(userId)),
    onMutate: async (next) => {
      await qc.cancelQueries({ queryKey: profileKey })
      const prev = qc.getQueryData<UserResponse>(profileKey)
      if (prev) {
        qc.setQueryData<UserResponse>(profileKey, {
          ...prev,
          followedByMe: next,
          followerCount: prev.followerCount + (next ? 1 : -1),
        })
      }
      return { prev }
    },
    onError: (_error, _next, context) => {
      if (context?.prev) qc.setQueryData(profileKey, context.prev)
    },
    onSettled: () => {
      qc.invalidateQueries({ queryKey: profileKey })
      qc.invalidateQueries({ queryKey: ['followers', userId] })
      qc.invalidateQueries({ queryKey: ['following', userId] })
    },
  })
}

export function useFollowers(id: number, page: number, options?: { enabled?: boolean }) {
  return useQuery({
    queryKey: ['followers', id, page],
    queryFn: () => getFollowers(id, page),
    placeholderData: keepPreviousData,
    enabled: options?.enabled ?? true,
  })
}

export function useFollowing(id: number, page: number, options?: { enabled?: boolean }) {
  return useQuery({
    queryKey: ['following', id, page],
    queryFn: () => getFollowing(id, page),
    placeholderData: keepPreviousData,
    enabled: options?.enabled ?? true,
  })
}
