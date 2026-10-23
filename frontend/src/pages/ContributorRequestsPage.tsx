import { useState } from 'react'
import { Link, Navigate } from 'react-router-dom'
import { ApiError } from '../api/client'
import type { ContributorRequestResponse, ContributorRequestStatus } from '../api/types'
import { Avatar } from '../components/Avatar'
import { Pagination } from '../components/Pagination'
import { RoleBadge } from '../components/RoleBadge'
import { EmptyState, ErrorState, Loading } from '../components/states'
import { useAuth } from '../auth/useAuth'
import {
  useContributorRequests,
  useResolveContributorRequest,
} from '../hooks/useContributorRequests'
import { formatDate } from '../lib/format'
import { isAtLeast } from '../lib/roles'
import { useToast } from '../toast/useToast'

const FILTERS: { value: '' | ContributorRequestStatus; label: string }[] = [
  { value: 'PENDING', label: 'Pendientes' },
  { value: 'APPROVED', label: 'Aprobadas' },
  { value: 'REJECTED', label: 'Rechazadas' },
  { value: '', label: 'Todas' },
]

const STATUS_LABEL: Record<ContributorRequestStatus, string> = {
  PENDING: 'Pendiente',
  APPROVED: 'Aprobada',
  REJECTED: 'Rechazada',
}

const STATUS_CLASS: Record<ContributorRequestStatus, string> = {
  PENDING: 'bg-sand-200 text-bark-700 ring-sand-300',
  APPROVED: 'bg-leaf-100 text-leaf-700 ring-leaf-300',
  REJECTED: 'bg-red-50 text-red-700 ring-red-200',
}

function RequestCard({ request }: { request: ContributorRequestResponse }) {
  const toast = useToast()
  const { approve, reject } = useResolveContributorRequest()
  const resolving = approve.isPending || reject.isPending

  function notifyError(err: unknown) {
    if (err instanceof ApiError && err.status === 409) toast.error('La petición ya fue resuelta.')
    else toast.error(err instanceof ApiError ? err.message : 'No se pudo resolver la petición.')
  }

  return (
    <div className="rounded-xl border border-sand-200 bg-cream-100 p-4 shadow-soft">
      <div className="mb-2 flex flex-wrap items-center gap-2">
        <Link
          to={`/users/${request.requester.username}`}
          className="flex items-center gap-2 hover:text-leaf-700"
        >
          <Avatar
            name={request.requester.username}
            src={request.requester.avatarUrl}
            size="sm"
          />
          <span className="font-semibold text-ink">{request.requester.username}</span>
        </Link>
        <RoleBadge role={request.requester.role} />
        <span
          className={`rounded-full px-2 py-0.5 text-xs font-semibold ring-1 ${STATUS_CLASS[request.status]}`}
        >
          {STATUS_LABEL[request.status]}
        </span>
        <span className="text-xs text-ink-faint">{formatDate(request.createdAt)}</span>
      </div>

      <p className="whitespace-pre-wrap break-words text-sm text-ink">{request.message}</p>

      <p className="mt-2 text-sm text-ink-soft">
        <span className="font-semibold text-ink">Contacto:</span> {request.contact}
      </p>

      {request.status === 'PENDING' ? (
        <div className="mt-3 flex gap-2">
          <button
            type="button"
            disabled={resolving}
            onClick={() =>
              approve.mutate(request.id, {
                onSuccess: () => toast.success(`${request.requester.username} ahora es Grow.`),
                onError: notifyError,
              })
            }
            className="btn-primary px-3 py-1.5 text-sm"
          >
            Aprobar
          </button>
          <button
            type="button"
            disabled={resolving}
            onClick={() =>
              reject.mutate(request.id, {
                onSuccess: () => toast.success('Petición rechazada.'),
                onError: notifyError,
              })
            }
            className="rounded-lg border border-red-200 px-3 py-1.5 text-sm font-semibold text-red-700 transition hover:bg-red-50 disabled:opacity-50"
          >
            Rechazar
          </button>
        </div>
      ) : (
        request.resolvedBy && (
          <p className="mt-3 text-xs text-ink-faint">
            Resuelta por{' '}
            <Link
              to={`/users/${request.resolvedBy.username}`}
              className="font-semibold text-ink hover:text-leaf-700"
            >
              {request.resolvedBy.username}
            </Link>
            {request.resolvedAt && <> · {formatDate(request.resolvedAt)}</>}
          </p>
        )
      )}
    </div>
  )
}

export function ContributorRequestsPage() {
  const { user, initializing } = useAuth()
  const [status, setStatus] = useState<'' | ContributorRequestStatus>('PENDING')
  const [page, setPage] = useState(0)

  const { data, isLoading, isError, error, refetch } = useContributorRequests(status || null, page)

  if (initializing) return <Loading />
  if (!isAtLeast(user?.role, 'MODERATOR')) return <Navigate to="/" replace />

  return (
    <div className="mx-auto max-w-2xl">
      <h1 className="mb-2 text-2xl font-semibold text-ink">Peticiones para ser Grow</h1>
      <p className="mb-4 text-sm text-ink-soft">
        Usuarios que piden ser promovidos a Grow (CONTRIBUTOR). Revisá el mensaje y el contacto;
        aprobar promueve a la persona al instante.
      </p>

      <select
        className="select mb-4"
        value={status}
        onChange={(e) => {
          setStatus(e.target.value as '' | ContributorRequestStatus)
          setPage(0)
        }}
      >
        {FILTERS.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>

      {isLoading && <Loading />}
      {isError && <ErrorState error={error} onRetry={refetch} />}

      {data && (
        <>
          {data.content.length === 0 ? (
            <EmptyState message="No hay peticiones en este estado." />
          ) : (
            <ul className="space-y-3">
              {data.content.map((request) => (
                <li key={request.id}>
                  <RequestCard request={request} />
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
