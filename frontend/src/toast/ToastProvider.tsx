import { useCallback, useMemo, useState, type ReactNode } from 'react'
import { ToastContext, type ToastType } from './ToastContext'

interface ToastItem {
  id: number
  type: ToastType
  message: string
}

let counter = 0

const STYLES: Record<ToastType, { border: string; icon: string }> = {
  success: { border: 'border-l-leaf-500', icon: '✓' },
  error: { border: 'border-l-red-500', icon: '⚠' },
  info: { border: 'border-l-bark-500', icon: 'ℹ' },
}

// Proveedor de toasts: cola efímera abajo-centro, auto-cierre a los 3.5s, clic para descartar.
export function ToastProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<ToastItem[]>([])

  const dismiss = useCallback((id: number) => {
    setItems((prev) => prev.filter((t) => t.id !== id))
  }, [])

  const show = useCallback(
    (type: ToastType, message: string) => {
      const id = ++counter
      setItems((prev) => [...prev, { id, type, message }])
      window.setTimeout(() => dismiss(id), 3500)
    },
    [dismiss],
  )

  const api = useMemo(
    () => ({
      success: (m: string) => show('success', m),
      error: (m: string) => show('error', m),
      info: (m: string) => show('info', m),
    }),
    [show],
  )

  return (
    <ToastContext.Provider value={api}>
      {children}
      <div className="pointer-events-none fixed inset-x-0 bottom-4 z-50 flex flex-col items-center gap-2 px-4">
        {items.map((t) => (
          <button
            key={t.id}
            type="button"
            role="status"
            onClick={() => dismiss(t.id)}
            className={`toast-enter pointer-events-auto flex w-full max-w-sm items-start gap-2 rounded-xl border border-sand-200 border-l-4 bg-cream-50 px-4 py-3 text-left text-sm text-ink shadow-raised ${STYLES[t.type].border}`}
          >
            <span aria-hidden className="font-semibold">
              {STYLES[t.type].icon}
            </span>
            <span className="flex-1">{t.message}</span>
          </button>
        ))}
      </div>
    </ToastContext.Provider>
  )
}
