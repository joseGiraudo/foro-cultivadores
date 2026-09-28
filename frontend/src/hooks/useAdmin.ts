import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { changeUserRole, changeUserStatus, listUsers, type ListUsersParams } from '../api/admin'
import { createCategory } from '../api/categories'

export function useAdminUsers(params: ListUsersParams) {
  return useQuery({
    queryKey: ['admin-users', params],
    queryFn: () => listUsers(params),
    placeholderData: keepPreviousData,
  })
}

export function useChangeUserRole() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, role }: { id: number; role: string }) => changeUserRole(id, role),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin-users'] }),
  })
}

export function useChangeUserStatus() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, status }: { id: number; status: string }) => changeUserStatus(id, status),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin-users'] }),
  })
}

export function useCreateCategory() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ name, description }: { name: string; description: string }) =>
      createCategory(name, description),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['categories'] }),
  })
}
