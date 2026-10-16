// Página placeholder para rutas que se implementan en hitos posteriores.
export function PlaceholderPage({ title }: { title: string }) {
  return (
    <div>
      <h1 className="mb-2 text-2xl font-semibold text-ink">{title}</h1>
      <p className="text-ink-faint">Pantalla pendiente (se implementa en un hito posterior).</p>
    </div>
  )
}
