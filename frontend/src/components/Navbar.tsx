import { useState } from 'react'
import { Link, NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import { useUnreadCount } from '../hooks/useNotifications'
import { isAtLeast } from '../lib/roles'
import { RoleBadge } from './RoleBadge'

export function Navbar() {
  const { user, isAuthenticated, logout } = useAuth()
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)
  const { data: unread } = useUnreadCount()
  const unreadCount = unread?.count ?? 0

  const linkClass = ({ isActive }: { isActive: boolean }) =>
    `px-3 py-2 rounded-lg text-sm font-semibold transition ${
      isActive ? 'bg-leaf-600 text-cream-50 shadow-soft' : 'text-ink-soft hover:bg-leaf-100 hover:text-leaf-700'
    }`

  function close() {
    setOpen(false)
  }

  function handleLogout() {
    close()
    logout()
    navigate('/')
  }

  // Links de navegación, compartidos entre la barra de escritorio y el panel mobile.
  // `onNavigate` cierra el menú en mobile al tocar un link (en desktop es un no-op).
  const items = (
    <>
      <NavLink to="/" className={linkClass} end onClick={close}>
        Inicio
      </NavLink>

      {isAuthenticated && user ? (
        <>
          {/* Se muestra a cualquier autenticado: un USER puede crear QUESTION;
              el backend decide si el tipo elegido requiere más rol (403). */}
          <NavLink to="/posts/new" className={linkClass} onClick={close}>
            Publicar
          </NavLink>
          <NavLink to="/saved" className={linkClass} onClick={close}>
            Guardados
          </NavLink>
          <NavLink to="/notifications" className={linkClass} onClick={close}>
            <span className="inline-flex items-center gap-1.5">
              🔔 Notificaciones
              {unreadCount > 0 && (
                <span className="inline-flex min-w-5 items-center justify-center rounded-full bg-leaf-600 px-1.5 text-xs font-bold text-cream-50">
                  {unreadCount > 99 ? '99+' : unreadCount}
                </span>
              )}
            </span>
          </NavLink>
          {isAtLeast(user.role, 'MODERATOR') && (
            <NavLink to="/moderation" className={linkClass} onClick={close}>
              Moderación
            </NavLink>
          )}
          {isAtLeast(user.role, 'ADMIN') && (
            <NavLink to="/admin" className={linkClass} onClick={close}>
              Admin
            </NavLink>
          )}
          <Link
            to={`/users/${user.username}`}
            onClick={close}
            className="flex items-center gap-2 rounded-lg px-3 py-2 text-sm transition hover:bg-leaf-100"
          >
            <span className="font-semibold text-ink">{user.username}</span>
            <RoleBadge role={user.role} />
          </Link>
          <button
            type="button"
            onClick={handleLogout}
            className="rounded-lg px-3 py-2 text-left text-sm font-semibold text-ink-faint transition hover:text-ink"
          >
            Salir
          </button>
        </>
      ) : (
        <>
          <NavLink to="/login" className={linkClass} onClick={close}>
            Ingresar
          </NavLink>
          <NavLink to="/register" className={linkClass} onClick={close}>
            Registrarse
          </NavLink>
        </>
      )}
    </>
  )

  return (
    <nav className="sticky top-0 z-10 border-b border-sand-200 bg-cream-100/90 backdrop-blur">
      <div className="mx-auto flex max-w-5xl items-center justify-between px-4 py-3">
        <NavLink
          to="/"
          onClick={close}
          className="font-display text-lg font-semibold text-leaf-700 sm:text-xl"
        >
          🌱 Foro de Cultivadores
        </NavLink>

        {/* Barra horizontal en escritorio */}
        <div className="hidden items-center gap-2 md:flex">{items}</div>

        {/* Botón hamburguesa en mobile */}
        <button
          type="button"
          aria-label={open ? 'Cerrar menú' : 'Abrir menú'}
          aria-expanded={open}
          onClick={() => setOpen((v) => !v)}
          className="rounded-lg p-2 text-ink-soft transition hover:bg-leaf-100 hover:text-leaf-700 md:hidden"
        >
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
            {open ? (
              <>
                <line x1="6" y1="6" x2="18" y2="18" />
                <line x1="6" y1="18" x2="18" y2="6" />
              </>
            ) : (
              <>
                <line x1="3" y1="6" x2="21" y2="6" />
                <line x1="3" y1="12" x2="21" y2="12" />
                <line x1="3" y1="18" x2="21" y2="18" />
              </>
            )}
          </svg>
        </button>
      </div>

      {/* Panel desplegable en mobile */}
      {open && (
        <div className="border-t border-sand-200 md:hidden">
          <div className="mx-auto flex max-w-5xl flex-col gap-1 px-4 py-3">{items}</div>
        </div>
      )}
    </nav>
  )
}
