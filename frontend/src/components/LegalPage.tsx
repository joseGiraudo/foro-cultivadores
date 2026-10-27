import type { ReactNode } from 'react'
import { LegalBody } from './LegalBody'

// Layout de página completa para los textos legales (términos, privacidad, normas). El cuerpo usa
// LegalBody, que también se reutiliza en el modal del registro.
export function LegalPage({
  title,
  updated,
  children,
}: {
  title: string
  updated?: string
  children: ReactNode
}) {
  return (
    <article className="mx-auto max-w-3xl">
      <h1 className="text-3xl font-semibold text-ink">{title}</h1>
      {updated && <p className="mt-1 text-xs text-ink-faint">Última actualización: {updated}</p>}
      <div className="mt-6">
        <LegalBody>{children}</LegalBody>
      </div>
    </article>
  )
}
