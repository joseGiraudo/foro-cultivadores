import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import { Avatar } from './Avatar'
import { RoleBadge } from './RoleBadge'

/**
 * Menú de usuario (escritorio): el avatar abre un dropdown con perfil, editar, guardados y salir.
 * Se cierra al clickear afuera, con Escape, o al navegar. Reemplaza los links sueltos del navbar.
 */
export function UserMenu() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)
  const ref = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!open) return
    function onPointer(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false)
    }
    function onKey(e: KeyboardEvent) {
      if (e.key === 'Escape') setOpen(false)
    }
    document.addEventListener('mousedown', onPointer)
    document.addEventListener('keydown', onKey)
    return () => {
      document.removeEventListener('mousedown', onPointer)
      document.removeEventListener('keydown', onKey)
    }
  }, [open])

  if (!user) return null

  function handleLogout() {
    setOpen(false)
    logout()
    navigate('/')
  }

  const itemClass =
    'block w-full rounded-lg px-3 py-2 text-left text-sm text-ink-soft transition hover:bg-leaf-100 hover:text-leaf-700'

  return (
    <div className="relative" ref={ref}>
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        aria-haspopup="menu"
        aria-expanded={open}
        className="flex items-center gap-2 rounded-lg px-2 py-1.5 transition hover:bg-leaf-100"
      >
        <Avatar name={user.username} src={user.avatarUrl} size="xs" />
        <span className="hidden font-semibold text-ink lg:inline">{user.username}</span>
        <svg
          width="16"
          height="16"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
          className={`text-ink-faint transition ${open ? 'rotate-180' : ''}`}
        >
          <polyline points="6 9 12 15 18 9" />
        </svg>
      </button>

      {open && (
        <div
          role="menu"
          className="absolute right-0 z-20 mt-2 w-52 rounded-xl border border-sand-200 bg-cream-50 p-1.5 shadow-raised"
        >
          <div className="flex items-center gap-2 px-3 py-2">
            <span className="font-semibold text-ink">{user.username}</span>
            <RoleBadge role={user.role} />
          </div>
          <div className="my-1 border-t border-sand-200" />
          <Link to={`/users/${user.username}`} role="menuitem" className={itemClass} onClick={() => setOpen(false)}>
            Ver perfil
          </Link>
          <Link to="/me" role="menuitem" className={itemClass} onClick={() => setOpen(false)}>
            Editar perfil
          </Link>
          <Link to="/saved" role="menuitem" className={itemClass} onClick={() => setOpen(false)}>
            Guardados
          </Link>
          <div className="my-1 border-t border-sand-200" />
          <button
            type="button"
            role="menuitem"
            onClick={handleLogout}
            className={`${itemClass} hover:bg-red-50 hover:text-red-700`}
          >
            Cerrar sesión
          </button>
        </div>
      )}
    </div>
  )
}
