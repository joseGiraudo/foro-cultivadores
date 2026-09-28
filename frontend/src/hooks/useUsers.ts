import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  followUser,
  getFollowers,
  getFollowing,
  getUser,
  unfollowUser,
  updateMe,
} from '../api/users'
import type { UserResponse } from '../api/types'
import { useAuth } from '../auth/useAuth'

export function useUser(id: number) {
  return useQuery({
    queryKey: ['user', id],
    queryFn: () => getUser(id),
  })
}

export function useUpdateMe() {
  const { refreshMe } = useAuth()
  return useMutation({
    mutationFn: (bio: string) => updateMe(bio),
    onSuccess: () => refreshMe(),
  })
}

// Seguir/dejar de seguir con actualización optimista sobre el perfil en cache.
export function useFollow(userId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (next: boolean) => (next ? followUser(userId) : unfollowUser(userId)),
    onMutate: async (next) => {
      await qc.cancelQueries({ queryKey: ['user', userId] })
      const prev = qc.getQueryData<UserResponse>(['user', userId])
      if (prev) {
        qc.setQueryData<UserResponse>(['user', userId], {
          ...prev,
          followedByMe: next,
          followerCount: prev.followerCount + (next ? 1 : -1),
        })
      }
      return { prev }
    },
    onError: (_error, _next, context) => {
      if (context?.prev) qc.setQueryData(['user', userId], context.prev)
    },
    onSettled: () => {
      qc.invalidateQueries({ queryKey: ['user', userId] })
      qc.invalidateQueries({ queryKey: ['followers', userId] })
      qc.invalidateQueries({ queryKey: ['following', userId] })
    },
  })
}

export function useFollowers(id: number, page: number) {
  return useQuery({
    queryKey: ['followers', id, page],
    queryFn: () => getFollowers(id, page),
    placeholderData: keepPreviousData,
  })
}

export function useFollowing(id: number, page: number) {
  return useQuery({
    queryKey: ['following', id, page],
    queryFn: () => getFollowing(id, page),
    placeholderData: keepPreviousData,
  })
}
