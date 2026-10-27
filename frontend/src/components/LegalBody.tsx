import type { ReactNode } from 'react'

// Cuerpo con estilo "prosa" para contenido legal. Estiliza los elementos hijos (h2/p/ul/a/strong)
// con variantes arbitrarias. Se usa en las páginas legales y en el modal del registro.
export function LegalBody({ children }: { children: ReactNode }) {
  return (
    <div className="space-y-3 text-sm leading-relaxed text-ink-soft [&_a]:text-leaf-700 [&_a]:underline [&_h2]:mb-1 [&_h2]:mt-7 [&_h2]:text-lg [&_h2]:font-semibold [&_h2]:text-ink [&_li]:mt-1 [&_strong]:text-ink [&_ul]:list-disc [&_ul]:space-y-1 [&_ul]:pl-6">
      {children}
    </div>
  )
}
