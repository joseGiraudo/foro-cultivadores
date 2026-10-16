import { api } from './client'
import type { NotificationResponse, Page } from './types'

// GET /notifications → página de notificaciones del usuario (más nuevas primero).
export function getNotifications(page = 0): Promise<Page<NotificationResponse>> {
  return api.get<Page<NotificationResponse>>('/notifications', { page })
}

// GET /notifications/unread-count → cantidad de no leídas (para el badge de la campana).
export function getUnreadCount(): Promise<{ count: number }> {
  return api.get<{ count: number }>('/notifications/unread-count')
}

// POST /notifications/{id}/read → marca una como leída (204).
export function markNotificationRead(id: number): Promise<void> {
  return api.post<void>(`/notifications/${id}/read`)
}

// POST /notifications/read-all → marca todas como leídas (204).
export function markAllNotificationsRead(): Promise<void> {
  return api.post<void>('/notifications/read-all')
}
