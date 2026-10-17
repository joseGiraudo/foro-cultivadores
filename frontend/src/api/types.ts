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
  // URL del avatar (o null si usa iniciales).
  avatarUrl: string | null
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
  // Cantidad total de usuarios que guardaron el post.
  saveCount: number
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
  // Cantidad total de usuarios que guardaron el post.
  saveCount: number
  createdAt: string
  updatedAt: string
  // Última edición de contenido; null si nunca se editó (los cambios de estado no cuentan).
  editedAt: string | null
}

// Autor embebido en comentarios (sin rol).
export interface CommentAuthor {
  id: number
  username: string
  // URL del avatar (o null si usa iniciales).
  avatarUrl: string | null
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
  // Datos de comercio/growshop (opcionales; null si no se cargaron o el rol no los permite).
  website: string | null
  phone: string | null
  instagram: string | null
  location: string | null
  // URLs de las imágenes del usuario (null si no subió). coverUrl solo lo completan los Grows.
  avatarUrl: string | null
  coverUrl: string | null
  role: Role
  status: UserStatus
  followerCount: number
  followingCount: number
  followedByMe: boolean | null
  createdAt: string
}

// Payload de PATCH /users/me. bio siempre; los de comercio solo los manda la UI si el rol alcanza.
export interface UpdateMeInput {
  bio: string
  website?: string
  phone?: string
  instagram?: string
  location?: string
}

export type NotificationType =
  | 'COMMENT_ON_POST'
  | 'REPLY_TO_COMMENT'
  | 'CONTRIBUTOR_REQUEST_APPROVED'
  | 'CONTRIBUTOR_REQUEST_REJECTED'

// Notificación in-app. actorUsername y postTitle son snapshots (point-in-time) del backend.
// postId/postTitle son null en notificaciones que no nacen de un post (p. ej. resultado de petición).
export interface NotificationResponse {
  id: number
  type: NotificationType
  actorUserId: number
  actorUsername: string
  postId: number | null
  postTitle: string | null
  commentId: number | null
  read: boolean
  createdAt: string
}

export type ContributorRequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

// Petición para ser CONTRIBUTOR (Grow). La crea un USER; la resuelve el staff (MODERATOR+).
export interface ContributorRequestResponse {
  id: number
  requester: {
    id: number
    username: string
    role: Role
    avatarUrl: string | null
  }
  message: string
  contact: string
  status: ContributorRequestStatus
  createdAt: string
  resolvedAt: string | null
  resolvedBy: { id: number; username: string } | null
}

export interface CreateContributorRequestInput {
  message: string
  contact: string
}
