import { Link, NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import { isAtLeast } from '../lib/roles'
import { RoleBadge } from './RoleBadge'

export function Navbar() {
  const { user, isAuthenticated, logout } = useAuth()
  const navigate = useNavigate()

  const linkClass = ({ isActive }: { isActive: boolean }) =>
    `px-3 py-2 rounded-md text-sm font-medium ${
      isActive ? 'bg-green-700 text-white' : 'text-gray-700 hover:bg-gray-100'
    }`

  function handleLogout() {
    logout()
    navigate('/')
  }

  return (
    <nav className="border-b border-gray-200 bg-white">
      <div className="mx-auto flex max-w-5xl items-center justify-between px-4 py-3">
        <NavLink to="/" className="text-lg font-bold text-green-800">
          🌱 Foro de Cultivadores
        </NavLink>
        <div className="flex items-center gap-2">
          <NavLink to="/" className={linkClass} end>
            Inicio
          </NavLink>

          {isAuthenticated && user ? (
            <>
              {/* Se muestra a cualquier autenticado: un USER puede crear QUESTION;
                  el backend decide si el tipo elegido requiere más rol (403). */}
              <NavLink to="/posts/new" className={linkClass}>
                Publicar
              </NavLink>
              {isAtLeast(user.role, 'MODERATOR') && (
                <NavLink to="/moderation" className={linkClass}>
                  Moderación
                </NavLink>
              )}
              {isAtLeast(user.role, 'ADMIN') && (
                <NavLink to="/admin" className={linkClass}>
                  Admin
                </NavLink>
              )}
              <Link
                to="/me"
                className="flex items-center gap-2 rounded-md px-3 py-2 text-sm hover:bg-gray-100"
              >
                <span className="font-medium text-gray-900">{user.username}</span>
                <RoleBadge role={user.role} />
              </Link>
              <button
                type="button"
                onClick={handleLogout}
                className="px-3 py-2 text-sm font-medium text-gray-500 hover:text-gray-800"
              >
                Salir
              </button>
            </>
          ) : (
            <>
              <NavLink to="/login" className={linkClass}>
                Ingresar
              </NavLink>
              <NavLink to="/register" className={linkClass}>
                Registrarse
              </NavLink>
            </>
          )}
        </div>
      </div>
    </nav>
  )
}
