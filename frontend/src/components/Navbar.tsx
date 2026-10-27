import { useState } from 'react'
import { Link, NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import { useContributorPendingCount } from '../hooks/useContributorRequests'
import { useUnreadCount } from '../hooks/useNotifications'
import { isAtLeast } from '../lib/roles'
import { Avatar } from './Avatar'
import { RoleBadge } from './RoleBadge'
import { SearchBar } from './SearchBar'
import { UserMenu } from './UserMenu'

export function Navbar() {
  const { user, isAuthenticated, logout } = useAuth()
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)
  const { data: unread } = useUnreadCount()
  const unreadCount = unread?.count ?? 0
  const { data: pending } = useContributorPendingCount()
  const pendingRequests = pending?.count ?? 0

  const linkClass = ({ isActive }: { isActive: boolean }) =>
    `px-3 py-2 rounded-lg text-sm font-semibold transition ${
      isActive ? 'bg-leaf-600 text-cream-50 shadow-soft' : 'text-ink-soft hover:bg-leaf-100 hover:text-leaf-700'
    }`

  function close() {
    setOpen(false)
  }

  function handleLogout() {
    close()
    void logout()
    navigate('/')
  }

  function badge(count: number) {
    if (count <= 0) return null
    return (
      <span className="inline-flex min-w-5 items-center justify-center rounded-full bg-leaf-600 px-1.5 text-xs font-bold text-cream-50">
        {count > 99 ? '99+' : count}
      </span>
    )
  }

  // Botón "Publicar" compacto (ícono + texto), acción primaria.
  const publishButton = (
    <NavLink
      to="/posts/new"
      onClick={close}
      className="inline-flex items-center gap-1.5 rounded-lg bg-leaf-600 px-3 py-1.5 text-sm font-semibold text-cream-50 shadow-soft transition hover:bg-leaf-700"
    >
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round">
        <line x1="12" y1="5" x2="12" y2="19" />
        <line x1="5" y1="12" x2="19" y2="12" />
      </svg>
      Publicar
    </NavLink>
  )

  // Acciones de escritorio a la derecha (autenticado): compactas, sin "Inicio" ni "Guardados".
  const desktopActions = user && (
    <>
      {publishButton}
      <NavLink
        to="/notifications"
        onClick={close}
        aria-label="Notificaciones"
        title="Notificaciones"
        className={({ isActive }) =>
          `relative rounded-lg p-2 text-lg transition ${
            isActive ? 'bg-leaf-600 text-cream-50 shadow-soft' : 'text-ink-soft hover:bg-leaf-100 hover:text-leaf-700'
          }`
        }
      >
        🔔
        {unreadCount > 0 && (
          <span className="absolute -right-0.5 -top-0.5 inline-flex min-w-4 items-center justify-center rounded-full bg-leaf-700 px-1 text-[10px] font-bold text-cream-50">
            {unreadCount > 99 ? '99+' : unreadCount}
          </span>
        )}
      </NavLink>
      {isAtLeast(user.role, 'MODERATOR') && (
        <NavLink to="/moderation" className={linkClass} onClick={close}>
          Moderación
        </NavLink>
      )}
      {isAtLeast(user.role, 'MODERATOR') && (
        <NavLink to="/contributor-requests" className={linkClass} onClick={close}>
          <span className="inline-flex items-center gap-1.5">Peticiones {badge(pendingRequests)}</span>
        </NavLink>
      )}
      {isAtLeast(user.role, 'ADMIN') && (
        <NavLink to="/admin" className={linkClass} onClick={close}>
          Admin
        </NavLink>
      )}
      <UserMenu />
    </>
  )

  // Lista plana para el panel mobile (incluye perfil, guardados y salir, que en escritorio van al dropdown).
  const mobileItems = (
    <>
      {isAuthenticated && user ? (
        <>
          <NavLink to="/posts/new" className={linkClass} onClick={close}>
            Publicar
          </NavLink>
          <NavLink to="/notifications" className={linkClass} onClick={close}>
            <span className="inline-flex items-center gap-1.5">🔔 Notificaciones {badge(unreadCount)}</span>
          </NavLink>
          {isAtLeast(user.role, 'MODERATOR') && (
            <NavLink to="/moderation" className={linkClass} onClick={close}>
              Moderación
            </NavLink>
          )}
          {isAtLeast(user.role, 'MODERATOR') && (
            <NavLink to="/contributor-requests" className={linkClass} onClick={close}>
              <span className="inline-flex items-center gap-1.5">Peticiones {badge(pendingRequests)}</span>
            </NavLink>
          )}
          {isAtLeast(user.role, 'ADMIN') && (
            <NavLink to="/admin" className={linkClass} onClick={close}>
              Admin
            </NavLink>
          )}

          <div className="my-1 border-t border-sand-200" />
          <Link
            to={`/users/${user.username}`}
            onClick={close}
            className="flex items-center gap-2 rounded-lg px-3 py-2 text-sm transition hover:bg-leaf-100"
          >
            <Avatar name={user.username} src={user.avatarUrl} size="xs" />
            <span className="font-semibold text-ink">{user.username}</span>
            <RoleBadge role={user.role} />
          </Link>
          <NavLink to="/me" className={linkClass} onClick={close}>
            Editar perfil
          </NavLink>
          <NavLink to="/saved" className={linkClass} onClick={close}>
            Guardados
          </NavLink>
          <button
            type="button"
            onClick={handleLogout}
            className="rounded-lg px-3 py-2 text-left text-sm font-semibold text-ink-faint transition hover:text-ink"
          >
            Cerrar sesión
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
      <div className="mx-auto flex max-w-6xl items-center justify-between gap-2 px-4 py-3">
        <NavLink
          to="/"
          onClick={close}
          className="shrink-0 font-display text-lg font-semibold text-leaf-700 sm:text-xl"
        >
          🌱 Foro de Cultivadores
        </NavLink>

        {/* Buscador global (escritorio): ocupa el espacio central */}
        <div className="mx-4 hidden max-w-sm flex-1 md:block">
          <SearchBar />
        </div>

        {/* Acciones en escritorio */}
        <div className="hidden items-center gap-2 md:flex">
          {isAuthenticated && user ? (
            desktopActions
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
          <div className="mx-auto flex max-w-6xl flex-col gap-1 px-4 py-3">
            <div className="mb-1">
              <SearchBar onNavigate={close} />
            </div>
            {mobileItems}
          </div>
        </div>
      )}
    </nav>
  )
}
