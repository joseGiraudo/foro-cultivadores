import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  getNotifications,
  getUnreadCount,
  markAllNotificationsRead,
  markNotificationRead,
} from '../api/notifications'
import { useAuth } from '../auth/useAuth'

export function useNotifications(page: number) {
  return useQuery({
    queryKey: ['notifications', page],
    queryFn: () => getNotifications(page),
    placeholderData: keepPreviousData,
  })
}

// Contador de no leídas para el badge de la campana. Solo con sesión; se refresca cada ~60s
// (y al volver el foco a la pestaña) para enterarse de notificaciones nuevas sin tiempo real.
export function useUnreadCount() {
  const { isAuthenticated } = useAuth()
  return useQuery({
    queryKey: ['notifications', 'unread-count'],
    queryFn: getUnreadCount,
    enabled: isAuthenticated,
    refetchInterval: 60_000,
  })
}

export function useMarkNotificationRead() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => markNotificationRead(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['notifications'] }),
  })
}

export function useMarkAllNotificationsRead() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: () => markAllNotificationsRead(),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['notifications'] }),
  })
}
