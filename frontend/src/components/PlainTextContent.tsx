import type { ReactNode } from 'react'

// URLs http(s) para autoenlazar. Solo esquemas seguros (nada de javascript:); React escapa el texto,
// así que no hay riesgo de XSS. El regex no cruza espacios/saltos, así que no se come varias líneas.
const URL_REGEX = /(https?:\/\/[^\s]+)/g

function linkify(text: string): ReactNode[] {
  const nodes: ReactNode[] = []
  let lastIndex = 0
  let match: RegExpExecArray | null
  URL_REGEX.lastIndex = 0
  while ((match = URL_REGEX.exec(text)) !== null) {
    const url = match[0]
    if (match.index > lastIndex) nodes.push(text.slice(lastIndex, match.index))
    nodes.push(
      <a
        key={match.index}
        href={url}
        target="_blank"
        rel="noopener noreferrer"
        className="text-leaf-700 underline"
      >
        {url}
      </a>,
    )
    lastIndex = match.index + url.length
  }
  if (lastIndex < text.length) nodes.push(text.slice(lastIndex))
  return nodes
}

// Render de texto plano (cuerpo de consultas): respeta saltos de línea/espacios (whitespace-pre-wrap)
// y autoenlaza URLs. No interpreta Markdown: lo que el usuario escribió se ve tal cual.
export function PlainTextContent({ content }: { content: string }) {
  return (
    <div className="whitespace-pre-wrap break-words leading-relaxed text-ink">{linkify(content)}</div>
  )
}
