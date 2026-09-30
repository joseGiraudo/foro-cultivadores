// Tipos que reflejan los DTOs de la API del backend (ver docs/mvp-spec.md §7).
// Se irán completando por hito; acá van los transversales del Hito 1.

// Página de Spring Data (listados paginados).
export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number // página actual (base 0)
  size: number
  first: boolean
  last: boolean
}

// RFC 9457 ProblemDetail que devuelve el backend en los errores.
export interface ProblemDetail {
  type?: string
  title?: string
  status: number
  detail?: string
  instance?: string
  // En validaciones (400), errores por campo.
  errors?: Record<string, string>
}

export type Role = 'USER' | 'CONTRIBUTOR' | 'MODERATOR' | 'ADMIN'
export type UserStatus = 'ACTIVE' | 'SUSPENDED'
export type PostType = 'QUESTION' | 'ARTICLE' | 'GUIDE'
export type ContentStatus = 'ACTIVE' | 'HIDDEN' | 'DELETED_BY_USER' | 'DELETED_BY_MOD'
export type VoteValue = 'UPVOTE' | 'DOWNVOTE'

// Resultado de votar: score actualizado + voto del usuario.
export interface VoteResponse {
  score: number
  myVote: VoteValue | null
}

export interface Category {
  id: number
  name: string
  description: string | null
}

// Autor embebido en respuestas de posts: id, username y rol.
export interface AuthorResponse {
  id: number
  username: string
  role: Role
}

// Categoría embebida en un post (solo id + name).
export interface CategoryView {
  id: number
  name: string
}

// Imagen de un post. `url` apunta a /images/{id} (relativa; resolver con resolveImageUrl).
export interface ImageResponse {
  id: number
  url: string
  position: number
  contentType: string
  sizeBytes: number
}

// Resumen de post para listados: sin contenido completo (solo `excerpt`) ni imágenes,
// pero con `thumbnailUrl` (imagen de menor position, null si no tiene).
export interface PostSummaryResponse {
  id: number
  title: string
  excerpt: string
  postType: PostType
  status: ContentStatus
  author: AuthorResponse
  categories: CategoryView[]
  thumbnailUrl: string | null
  score: number
  commentCount: number
  myVote: VoteValue | null
  // Si el usuario actual guardó el post (false si es anónimo).
  saved: boolean
  createdAt: string
  updatedAt: string
  // Última edición de contenido; null si nunca se editó (los cambios de estado no cuentan).
  editedAt: string | null
}

// Detalle de post: incluye `content` (Markdown crudo) e `images` ordenadas por position.
export interface PostResponse {
  id: number
  title: string
  content: string
  postType: PostType
  status: ContentStatus
  author: AuthorResponse
  categories: CategoryView[]
  images: ImageResponse[]
  score: number
  // Conteos desglosados (solo en el detalle del post); score === upvotes - downvotes.
  upvotes: number
  downvotes: number
  commentCount: number
  myVote: VoteValue | null
  // Si el usuario actual guardó el post (false si es anónimo).
  saved: boolean
  createdAt: string
  updatedAt: string
  // Última edición de contenido; null si nunca se editó (los cambios de estado no cuentan).
  editedAt: string | null
}

// Autor embebido en comentarios (sin rol).
export interface CommentAuthor {
  id: number
  username: string
}

// Ítem de la lista plana de comentarios (recorrido en profundidad).
// `content` es null si el comentario no es visible para quien consulta (no ACTIVE y no moderador);
// `status` siempre viene para no romper el hilo. `parentCommentId` y `depth` arman el árbol.
export interface CommentResponse {
  id: number
  content: string | null
  status: ContentStatus
  author: CommentAuthor
  parentCommentId: number | null
  depth: number
  score: number
  myVote: VoteValue | null
  // Si es el comentario fijado por el autor del post (uno por post). Los fijados van primero en la lista.
  pinned: boolean
  createdAt: string
  updatedAt: string
  // Última edición de contenido; null si nunca se editó (los cambios de estado no cuentan).
  editedAt: string | null
}

// Perfil de usuario. `email` solo viene en /users/me y en endpoints de admin (null en perfil público).
// `followedByMe` es null si quien consulta es anónimo.
export interface UserResponse {
  id: number
  username: string
  email: string | null
  bio: string | null
  role: Role
  status: UserStatus
  followerCount: number
  followingCount: number
  followedByMe: boolean | null
  createdAt: string
}
