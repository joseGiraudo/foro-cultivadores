// Página placeholder para rutas que se implementan en hitos posteriores.
export function PlaceholderPage({ title }: { title: string }) {
  return (
    <div>
      <h1 className="mb-2 text-2xl font-bold text-gray-900">{title}</h1>
      <p className="text-gray-500">Pantalla pendiente (se implementa en un hito posterior).</p>
    </div>
  )
}
