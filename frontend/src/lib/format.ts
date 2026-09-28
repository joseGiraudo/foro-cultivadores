// El backend sirve las imágenes en /images/{id} (ruta relativa). En dev, todo pasa por el
// proxy de Vite bajo /api, así que la URL efectiva es /api/images/{id}.
export function resolveImageUrl(url: string): string {
  return `/api${url}`
}

const dateFormatter = new Intl.DateTimeFormat('es-AR', {
  day: '2-digit',
  month: 'short',
  year: 'numeric',
})

// Formatea un Instant ISO (ej. "2026-09-26T12:00:00Z") a algo legible en español.
export function formatDate(iso: string): string {
  return dateFormatter.format(new Date(iso))
}
