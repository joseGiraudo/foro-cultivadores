import type { Components } from 'react-markdown'
import Markdown, { defaultUrlTransform } from 'react-markdown'
import rehypeSanitize from 'rehype-sanitize'
import remarkGfm from 'remark-gfm'
import { resolveImageUrl } from '../lib/format'

// Reescribe las URLs internas de imágenes (/images/{id}) para que pasen por el proxy (/api/...),
// preservando el saneo de URLs por defecto de react-markdown (bloquea javascript:, etc.).
function urlTransform(url: string): string {
  const safe = defaultUrlTransform(url)
  return safe.startsWith('/images/') ? resolveImageUrl(safe) : safe
}

// Estilos mínimos por elemento (no usamos @tailwindcss/typography).
const components: Components = {
  h1: (props) => <h1 className="mb-3 mt-5 text-2xl font-bold text-ink" {...props} />,
  h2: (props) => <h2 className="mb-2 mt-5 text-xl font-bold text-ink" {...props} />,
  h3: (props) => <h3 className="mb-2 mt-4 text-lg font-semibold text-ink" {...props} />,
  p: (props) => <p className="my-3 leading-relaxed text-ink" {...props} />,
  a: (props) => (
    <a className="text-leaf-700 underline" target="_blank" rel="noopener noreferrer" {...props} />
  ),
  ul: (props) => <ul className="my-3 list-disc space-y-1 pl-6 text-ink" {...props} />,
  ol: (props) => <ol className="my-3 list-decimal space-y-1 pl-6 text-ink" {...props} />,
  blockquote: (props) => (
    <blockquote className="my-3 border-l-4 border-sand-300 pl-4 italic text-ink-soft" {...props} />
  ),
  code: (props) => <code className="rounded bg-sand-200 px-1 py-0.5 text-sm" {...props} />,
  pre: (props) => (
    <pre className="my-3 overflow-x-auto rounded-md bg-sand-200 p-3 text-sm" {...props} />
  ),
  img: (props) => (
    <img className="my-3 max-w-full rounded-md" loading="lazy" alt={props.alt ?? ''} {...props} />
  ),
  // Las tablas de GFM pueden ser más anchas que la pantalla: las envolvemos en un
  // contenedor con scroll horizontal para no romper el layout en mobile.
  table: (props) => (
    <div className="my-3 overflow-x-auto">
      <table className="w-full border-collapse text-sm" {...props} />
    </div>
  ),
  th: (props) => <th className="border border-sand-200 px-3 py-1 text-left font-semibold" {...props} />,
  td: (props) => <td className="border border-sand-200 px-3 py-1" {...props} />,
}

// Renderiza Markdown crudo del backend. La sanitización (rehype-sanitize) es OBLIGATORIA:
// es la defensa anti-XSS del front.
export function MarkdownContent({ content }: { content: string }) {
  return (
    <div className="break-words text-ink">
      <Markdown
        remarkPlugins={[remarkGfm]}
        rehypePlugins={[rehypeSanitize]}
        urlTransform={urlTransform}
        components={components}
      >
        {content}
      </Markdown>
    </div>
  )
}
