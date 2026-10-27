// Modelo de bloques del editor visual de artículos/guías. Es solo una representación de edición:
// el contenido se guarda/renderiza SIEMPRE como Markdown (serialize/parse abajo), así el backend y
// el render publicado no cambian. Un bloque de texto contiene Markdown crudo (párrafo, título,
// lista, etc.); un bloque de imagen es una imagen sola (`![alt](url)`).

export type Block =
  | { id: string; type: 'text'; text: string }
  | { id: string; type: 'image'; url: string; alt: string; imageId: number | null }

// Imagen "sola" en su propio párrafo: `![alt](url)`. Si un párrafo no matchea, queda como texto
// (y si tenía un `![]()` embebido entre medio, igual se renderiza como imagen al publicar).
const LONE_IMAGE = /^!\[([^\]]*)\]\(([^)\s]+)\)$/
const IMAGE_ID_IN_URL = /\/images\/(\d+)/

let counter = 0
function newId(): string {
  counter += 1
  return `b${counter}-${Date.now()}`
}

export function imageIdFromUrl(url: string): number | null {
  const match = IMAGE_ID_IN_URL.exec(url)
  return match ? Number(match[1]) : null
}

export function makeTextBlock(text = ''): Block {
  return { id: newId(), type: 'text', text }
}

export function makeImageBlock(url: string, alt = ''): Block {
  return { id: newId(), type: 'image', url, alt, imageId: imageIdFromUrl(url) }
}

// Markdown → bloques. Separa por líneas en blanco; cada trozo es imagen (si es una imagen sola) o
// texto. Contenido vacío → un bloque de texto vacío para que haya dónde empezar a escribir.
export function markdownToBlocks(markdown: string): Block[] {
  const chunks = markdown
    .split(/\n{2,}/)
    .map((chunk) => chunk.trim())
    .filter((chunk) => chunk.length > 0)

  if (chunks.length === 0) return [makeTextBlock()]

  return chunks.map((chunk) => {
    const image = LONE_IMAGE.exec(chunk)
    return image ? makeImageBlock(image[2], image[1]) : makeTextBlock(chunk)
  })
}

// Bloques → Markdown. Descarta bloques de texto vacíos; une con línea en blanco entre párrafos.
export function blocksToMarkdown(blocks: Block[]): string {
  return blocks
    .map((block) => (block.type === 'image' ? `![${block.alt}](${block.url})` : block.text.trim()))
    .filter((piece) => piece.length > 0)
    .join('\n\n')
}
