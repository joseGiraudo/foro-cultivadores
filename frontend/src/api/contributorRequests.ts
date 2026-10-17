import { api } from './client'
import type {
  ContributorRequestResponse,
  ContributorRequestStatus,
  CreateContributorRequestInput,
  Page,
} from './types'

// POST /contributor-requests → crea una petición para ser CONTRIBUTOR (201).
export function createContributorRequest(
  input: CreateContributorRequestInput,
): Promise<ContributorRequestResponse> {
  return api.post<ContributorRequestResponse>('/contributor-requests', input)
}

// GET /contributor-requests/me → historial propio del solicitante (más reciente primero).
export function getMyContributorRequests(): Promise<ContributorRequestResponse[]> {
  return api.get<ContributorRequestResponse[]>('/contributor-requests/me')
}

// GET /contributor-requests (staff) → bandeja paginada, filtrable por estado.
export function listContributorRequests(
  status: ContributorRequestStatus | null,
  page = 0,
): Promise<Page<ContributorRequestResponse>> {
  return api.get<Page<ContributorRequestResponse>>('/contributor-requests', {
    status: status ?? undefined,
    page,
  })
}

// GET /contributor-requests/pending-count (staff) → conteo de pendientes para el badge.
export function getContributorPendingCount(): Promise<{ count: number }> {
  return api.get<{ count: number }>('/contributor-requests/pending-count')
}

// POST /contributor-requests/{id}/approve (staff) → aprueba y promueve al solicitante.
export function approveContributorRequest(id: number): Promise<ContributorRequestResponse> {
  return api.post<ContributorRequestResponse>(`/contributor-requests/${id}/approve`)
}

// POST /contributor-requests/{id}/reject (staff) → rechaza la petición.
export function rejectContributorRequest(id: number): Promise<ContributorRequestResponse> {
  return api.post<ContributorRequestResponse>(`/contributor-requests/${id}/reject`)
}
