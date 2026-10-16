import { useState } from 'react'
import { Link } from 'react-router-dom'
import type { NotificationResponse } from '../api/types'
import { Avatar } from '../components/Avatar'
import { Pagination } from '../components/Pagination'
import { EmptyState, ErrorState, Loading } from '../components/states'
import {
  useMarkAllNotificationsRead,
  useMarkNotificationRead,
  useNotifications,
} from '../hooks/useNotifications'
import { formatDate } from '../lib/format'

function actionText(type: NotificationResponse['type']): string {
  return type === 'REPLY_TO_COMMENT' ? 'respondió tu comentario en' : 'comentó tu publicación'
}

function NotificationItem({ notification }: { notification: NotificationResponse }) {
  const markRead = useMarkNotificationRead()

  return (
    <Link
      to={`/posts/${notification.postId}`}
      onClick={() => {
        if (!notification.read) markRead.mutate(notification.id)
      }}
      className={`flex items-start gap-3 rounded-xl border p-3 shadow-soft transition hover:border-leaf-300 hover:shadow-raised ${
        notification.read ? 'border-sand-200 bg-cream-100' : 'border-leaf-300 bg-leaf-100/50'
      }`}
    >
      <Avatar name={notification.actorUsername} size="sm" />
      <div className="min-w-0 flex-1">
        <p className="text-sm text-ink">
          <span className="font-semibold">{notification.actorUsername}</span>{' '}
          {actionText(notification.type)}{' '}
          <span className="font-semibold">«{notification.postTitle}»</span>
        </p>
        <p className="mt-0.5 text-xs text-ink-faint">{formatDate(notification.createdAt)}</p>
      </div>
      {!notification.read && (
        <span className="mt-1 h-2.5 w-2.5 shrink-0 rounded-full bg-leaf-600" aria-label="No leída" />
      )}
    </Link>
  )
}

export function NotificationsPage() {
  const [page, setPage] = useState(0)
  const { data, isLoading, isError, error, refetch } = useNotifications(page)
  const markAll = useMarkAllNotificationsRead()

  const hasUnread = data?.content.some((n) => !n.read) ?? false

  return (
    <div className="mx-auto max-w-2xl">
      <div className="mb-4 flex items-center justify-between gap-3">
        <h1 className="text-2xl font-semibold text-ink">Notificaciones</h1>
        <button
          type="button"
          onClick={() => markAll.mutate()}
          disabled={markAll.isPending || !hasUnread}
          className="rounded-lg border border-sand-200 px-3 py-1.5 text-sm font-semibold text-ink-soft transition hover:bg-leaf-100 hover:text-leaf-700 disabled:opacity-40"
        >
          Marcar todas como leídas
        </button>
      </div>

      {isLoading && <Loading />}
      {isError && <ErrorState error={error} onRetry={refetch} />}

      {data && (
        <>
          {data.content.length === 0 ? (
            <EmptyState message="No tenés notificaciones todavía." />
          ) : (
            <ul className="space-y-2">
              {data.content.map((notification) => (
                <li key={notification.id}>
                  <NotificationItem notification={notification} />
                </li>
              ))}
            </ul>
          )}
          <Pagination page={data.number} totalPages={data.totalPages} onChange={setPage} />
        </>
      )}
    </div>
  )
}
