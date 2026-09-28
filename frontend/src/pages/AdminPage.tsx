import { useState, type FormEvent } from 'react'
import { Navigate } from 'react-router-dom'
import { ApiError } from '../api/client'
import type { Role, UserResponse, UserStatus } from '../api/types'
import { Pagination } from '../components/Pagination'
import { EmptyState, ErrorState, Loading } from '../components/states'
import { useAuth } from '../auth/useAuth'
import {
  useAdminUsers,
  useChangeUserRole,
  useChangeUserStatus,
  useCreateCategory,
} from '../hooks/useAdmin'
import { useCategories } from '../hooks/useCategories'
import { isAtLeast } from '../lib/roles'

const ROLES: Role[] = ['USER', 'CONTRIBUTOR', 'MODERATOR', 'ADMIN']

function UserRow({ person, isSelf }: { person: UserResponse; isSelf: boolean }) {
  const changeRole = useChangeUserRole()
  const changeStatus = useChangeUserStatus()
  const [error, setError] = useState<string | null>(null)

  const suspended = person.status === 'SUSPENDED'

  function onError(err: unknown) {
    setError(err instanceof ApiError ? err.message : 'No se pudo actualizar.')
  }

  return (
    <tr className="border-t border-gray-200">
      <td className="px-3 py-2">
        <div className="font-medium text-gray-900">{person.username}</div>
        <div className="text-xs text-gray-500">{person.email}</div>
        {error && <div className="text-xs text-red-700">{error}</div>}
      </td>
      <td className="px-3 py-2">
        <select
          value={person.role}
          disabled={isSelf || changeRole.isPending}
          onChange={(e) => {
            setError(null)
            changeRole.mutate({ id: person.id, role: e.target.value }, { onError })
          }}
          className="rounded-md border border-gray-300 px-2 py-1 text-sm disabled:opacity-50"
        >
          {ROLES.map((role) => (
            <option key={role} value={role}>
              {role}
            </option>
          ))}
        </select>
      </td>
      <td className="px-3 py-2">
        <button
          type="button"
          disabled={isSelf || changeStatus.isPending}
          onClick={() => {
            setError(null)
            const next: UserStatus = suspended ? 'ACTIVE' : 'SUSPENDED'
            changeStatus.mutate({ id: person.id, status: next }, { onError })
          }}
          className={`rounded-md px-3 py-1 text-sm font-medium disabled:opacity-50 ${
            suspended
              ? 'bg-green-700 text-white hover:bg-green-800'
              : 'border border-red-300 text-red-700 hover:bg-red-50'
          }`}
        >
          {suspended ? 'Reactivar' : 'Suspender'}
        </button>
      </td>
    </tr>
  )
}

function UsersSection() {
  const { user } = useAuth()
  const [role, setRole] = useState<string>('')
  const [status, setStatus] = useState<string>('')
  const [page, setPage] = useState(0)
  const { data, isLoading, isError, error, refetch } = useAdminUsers({
    role: role || undefined,
    status: status || undefined,
    page,
  })

  const selectClass = 'rounded-md border border-gray-300 px-3 py-2 text-sm'

  return (
    <section className="mb-10">
      <h2 className="mb-3 text-lg font-semibold text-gray-900">Usuarios</h2>
      <div className="mb-3 flex gap-3">
        <select
          className={selectClass}
          value={role}
          onChange={(e) => {
            setRole(e.target.value)
            setPage(0)
          }}
        >
          <option value="">Todos los roles</option>
          {ROLES.map((r) => (
            <option key={r} value={r}>
              {r}
            </option>
          ))}
        </select>
        <select
          className={selectClass}
          value={status}
          onChange={(e) => {
            setStatus(e.target.value)
            setPage(0)
          }}
        >
          <option value="">Todos los estados</option>
          <option value="ACTIVE">Activos</option>
          <option value="SUSPENDED">Suspendidos</option>
        </select>
      </div>

      {isLoading && <Loading />}
      {isError && <ErrorState error={error} onRetry={refetch} />}

      {data && (
        <>
          {data.content.length === 0 ? (
            <EmptyState message="No hay usuarios con ese filtro." />
          ) : (
            <div className="overflow-x-auto rounded-md border border-gray-200 bg-white">
              <table className="w-full text-left text-sm">
                <thead className="bg-gray-50 text-xs uppercase text-gray-500">
                  <tr>
                    <th className="px-3 py-2">Usuario</th>
                    <th className="px-3 py-2">Rol</th>
                    <th className="px-3 py-2">Estado</th>
                  </tr>
                </thead>
                <tbody>
                  {data.content.map((person) => (
                    <UserRow key={person.id} person={person} isSelf={person.id === user?.id} />
                  ))}
                </tbody>
              </table>
            </div>
          )}
          <Pagination page={data.number} totalPages={data.totalPages} onChange={setPage} />
        </>
      )}
    </section>
  )
}

function CategoriesSection() {
  const { data: categories } = useCategories()
  const create = useCreateCategory()
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [error, setError] = useState<string | null>(null)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    if (name.trim().length === 0) return
    try {
      await create.mutateAsync({ name, description })
      setName('')
      setDescription('')
    } catch (err) {
      if (err instanceof ApiError && err.status === 409) {
        setError('Ya existe una categoría con ese nombre.')
      } else {
        setError(err instanceof ApiError ? err.message : 'No se pudo crear la categoría.')
      }
    }
  }

  return (
    <section>
      <h2 className="mb-3 text-lg font-semibold text-gray-900">Categorías</h2>
      <div className="mb-4 flex flex-wrap gap-2">
        {categories?.map((category) => (
          <span
            key={category.id}
            className="rounded-full bg-gray-100 px-3 py-1 text-sm text-gray-700"
          >
            {category.name}
          </span>
        ))}
      </div>

      <form onSubmit={handleSubmit} className="max-w-md space-y-2">
        <input
          type="text"
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="Nombre"
          maxLength={50}
          className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-green-600 focus:outline-none"
        />
        <input
          type="text"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          placeholder="Descripción (opcional)"
          maxLength={300}
          className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-green-600 focus:outline-none"
        />
        {error && <p className="text-sm text-red-700">{error}</p>}
        <button
          type="submit"
          disabled={create.isPending || name.trim().length === 0}
          className="rounded-md bg-green-700 px-4 py-2 font-medium text-white hover:bg-green-800 disabled:opacity-50"
        >
          {create.isPending ? 'Creando…' : 'Crear categoría'}
        </button>
      </form>
    </section>
  )
}

export function AdminPage() {
  const { user, initializing } = useAuth()

  if (initializing) return <Loading />
  if (!isAtLeast(user?.role, 'ADMIN')) return <Navigate to="/" replace />

  return (
    <div>
      <h1 className="mb-6 text-2xl font-bold text-gray-900">Administración</h1>
      <UsersSection />
      <CategoriesSection />
    </div>
  )
}
