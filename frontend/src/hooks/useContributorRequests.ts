import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  approveContributorRequest,
  createContributorRequest,
  getContributorPendingCount,
  getMyContributorRequests,
  listContributorRequests,
  rejectContributorRequest,
} from '../api/contributorRequests'
import type { ContributorRequestStatus, CreateContributorRequestInput } from '../api/types'
import { useAuth } from '../auth/useAuth'
import { isAtLeast } from '../lib/roles'

// Historial propio del solicitante. Solo tiene sentido para un USER (los Grow+ no piden).
export function useMyContributorRequests() {
  const { user } = useAuth()
  return useQuery({
    queryKey: ['contributor-requests', 'me'],
    queryFn: getMyContributorRequests,
    enabled: user?.role === 'USER',
  })
}

export function useCreateContributorRequest() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (input: CreateContributorRequestInput) => createContributorRequest(input),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['contributor-requests', 'me'] }),
  })
}

// Conteo de pendientes para el badge de la navbar. Solo staff; se refresca cada ~60s.
export function useContributorPendingCount() {
  const { user } = useAuth()
  return useQuery({
    queryKey: ['contributor-requests', 'pending-count'],
    queryFn: getContributorPendingCount,
    enabled: !!user && isAtLeast(user.role, 'MODERATOR'),
    refetchInterval: 60_000,
  })
}

// Bandeja del staff, paginada y filtrable por estado.
export function useContributorRequests(status: ContributorRequestStatus | null, page: number) {
  return useQuery({
    queryKey: ['contributor-requests', 'list', status, page],
    queryFn: () => listContributorRequests(status, page),
    placeholderData: keepPreviousData,
  })
}

// Aprobar/rechazar: invalida la bandeja y el contador de pendientes.
export function useResolveContributorRequest() {
  const qc = useQueryClient()
  const invalidate = () => {
    qc.invalidateQueries({ queryKey: ['contributor-requests', 'list'] })
    qc.invalidateQueries({ queryKey: ['contributor-requests', 'pending-count'] })
  }
  const approve = useMutation({
    mutationFn: (id: number) => approveContributorRequest(id),
    onSuccess: invalidate,
  })
  const reject = useMutation({
    mutationFn: (id: number) => rejectContributorRequest(id),
    onSuccess: invalidate,
  })
  return { approve, reject }
}
