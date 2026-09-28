# Ideas para el frontend (React)

> Documento de **ideas / inventario**, no un spec. Sirve para pensar qué pantallas y componentes
> necesita el front del MVP, mapeados contra los endpoints que **ya existen** en el backend.
> Más adelante, a partir de acá, se arma el spec del frontend y recién después el desarrollo.
>
> Base: el backend del MVP ya está completo (ver `mvp-spec.md`, `decisions.md`, `open_questions.md`).
> Todo esto se apoya en esos endpoints; no hay que inventar funcionalidad que el backend no ofrece.

## 1. Alcance del front del MVP

Reflejar los 4 escenarios que ya prueba el backend:
1. Un usuario publica una **consulta** (QUESTION) con foto; otros responden y las mejores respuestas (por votos) aparecen primero.
2. Un grow publica un **artículo** (ARTICLE) con párrafos e imágenes; la gente lo vota.
3. Un grow publica una **guía** (GUIDE) para principiantes; la gente la vota y comenta.
4. Un usuario entra al **perfil de un grow**, ve sus publicaciones y lo sigue.

Fuera del MVP (el backend tampoco los tiene todavía): feed de seguidos, notificaciones, búsqueda, catálogos, denuncias, cuentas de negocio, verificación. **El front no debería asumirlos.**

## 2. Consideraciones técnicas transversales (decidir en el spec)

- **Stack**: React + Vite + TypeScript (sugerido). Router (React Router). Manejo de estado del servidor: TanStack Query (encaja muy bien con esta API REST paginada). UI: a elección (Tailwind, MUI, etc.).
- **Autenticación = HTTP Basic (temporal).** El backend valida `Authorization: Basic ...` en cada request y lee rol/estado de la base cada vez. Implicancias:
  - No hay endpoint de login: se valida guardando las credenciales y pegándole a `GET /users/me`.
  - No hay logout de servidor; "cerrar sesión" = olvidar credenciales en el cliente.
  - Guardar credenciales de forma acotada (en memoria / sessionStorage); tener en cuenta que se mandan en cada request. Se reemplazará por JWT más adelante → **aislar el manejo de auth** en una capa (interceptor/cliente HTTP) para poder cambiarlo sin tocar todo.
- **Markdown**: el backend guarda el contenido de posts **en Markdown crudo, sin renderizar**. El front debe **renderizarlo y SANITIZARLO** (p. ej. `react-markdown` + `rehype-sanitize`). Es responsabilidad del front evitar XSS.
- **Imágenes**: las URLs vienen como `/images/{id}` (relativas). El front las resuelve contra la base del backend. En artículos/guías, las imágenes van referenciadas dentro del Markdown (`![desc](/images/{id})`); en consultas se muestran aparte (galería) ordenadas por `position`.
- **Errores**: la API responde con `ProblemDetail` (RFC 9457): `status`, `detail`, y en validaciones un objeto `errors` por campo. Conviene un manejador central que traduzca 400/401/403/404/409/413/415 a mensajes de UI.
- **Paginación**: los listados devuelven `Page` de Spring (`content`, `totalElements`, `totalPages`, `number`, `size`, ...). Tamaño por defecto 20, máximo 100. Params `?page=&size=&sort=`.
- **Roles y estado en la UI**: `USER < CONTRIBUTOR < MODERATOR < ADMIN`. La UI debería **ocultar/mostrar acciones según el rol** del usuario logueado, pero recordando que la autorización real la hace el backend (la UI es solo comodidad). Los mínimos por acción son **configurables** en el backend (`forum.rules.*`), así que idealmente el front no hardcodea reglas: muestra la acción e interpreta un 403 elegantemente. (A evaluar en el spec: exponer las reglas efectivas vía un endpoint de "capabilities". Hoy no existe.)
- **CORS**: el backend todavía **no** tiene CORS configurado. Si el front corre en otro origen (Vite en :5173), habrá que agregar config CORS en el backend (o un proxy de dev en Vite). Anotar como dependencia.

## 3. Pantallas / vistas → endpoints

### Registro y sesión
- **Registro** → `POST /auth/register` `{ username, email, password }`. Validaciones espejo del backend (username 3–30 `[a-zA-Z0-9_]`, email válido, password ≥ 8). Manejar 409 (username/email en uso).
- **Login** (form usuario/contraseña) → probar credenciales con `GET /users/me`; si 200, guardar sesión; si 401, error.
- **Estado de sesión**: navbar con usuario logueado (de `/users/me`), su rol, y avisos si está `SUSPENDED` (puede leer, no escribir → la UI puede avisar).

### Home / listado de posts
- **Feed general (no personalizado)** → `GET /posts` con filtros `?categoryId= &authorId= &postType= &status=` (status solo moderadores) y paginación.
  - Tarjeta de post (usa `PostSummaryResponse`): título, extracto, tipo, autor (username, rol), categorías, `thumbnailUrl`, `score`, `commentCount`, `myVote`, fecha.
  - Filtros por **categoría** (`GET /categories`) y por **tipo** (QUESTION/ARTICLE/GUIDE).
  - Orden por defecto: `createdAt` desc. (A futuro: orden por score, pero eso hoy es por endpoint de posts sin sort por score — evaluar.)
- **Badges por tipo** (Consulta / Artículo / Guía) para distinguir visualmente.

### Detalle de post
- `GET /posts/{id}` (`PostResponse`): título, **contenido Markdown renderizado**, tipo, estado, autor, categorías, **galería de imágenes** (ordenadas por `position`), `score`, `commentCount`, `myVote`, fechas.
- **Votar el post** → `PUT /posts/{id}/vote { value: UPVOTE|DOWNVOTE }` y `DELETE /posts/{id}/vote` (quitar). La respuesta trae `{ score, myVote }` para actualizar la UI al instante. Ocultar/deshabilitar si es el propio post (auto-voto → 403).
- **Sección de comentarios** (ver abajo).
- Acciones del **autor**: botón editar (`PATCH /posts/{id}`), eliminar (`PATCH /posts/{id}/status { DELETED_BY_USER }`), gestionar imágenes.
- Acciones de **moderador**: ocultar/restaurar/eliminar (`PATCH /posts/{id}/status` → HIDDEN / ACTIVE / DELETED_BY_MOD).

### Crear / editar post
- **Crear** → `POST /posts { title, content, postType, categoryIds }` (1–3 categorías).
  - El selector de tipo puede condicionar quién puede crear (ARTICLE/GUIDE requieren CONTRIBUTOR+ por config); si el rol no alcanza, el backend responde 403 → mostrarlo claro.
  - Editor de **Markdown** con preview.
  - Selector de categorías (multi, 1–3).
- **Subir imágenes** (flujo en dos pasos): primero se crea el post, después `POST /posts/{postId}/images` (multipart, campo `file`, una por request). Para artículos/guías, tras subir se **inserta la referencia** `![desc](/images/{id})` en el Markdown. Validar en UI tamaño (≤ 5MB) y tipo (jpeg/png/webp) para feedback rápido, pero el backend igual valida por contenido real (413/415).
  - **Ojo**: un WebP subido se guarda/sirve como PNG (el backend lo re-codifica). El front debería usar la `url`/`contentType` que devuelve la API, no asumir el formato original.
- **Eliminar imagen** → `DELETE /posts/{postId}/images/{imageId}`.
- **Editar** → `PATCH /posts/{id} { title?, content?, categoryIds? }` (solo autor, solo si ACTIVE; el tipo no se cambia).

### Comentarios (dentro del detalle de post)
- **Listado** → `GET /posts/{postId}/comments`: **lista plana en recorrido en profundidad**; cada ítem trae `parentCommentId` y `depth` → el front arma el árbol/indentado. Los no visibles vienen con `content: null` y su `status` (mostrar "comentario oculto/eliminado" sin romper el hilo).
  - Orden: los de primer nivel según el tipo de post (QUESTION por **score**, ARTICLE/GUIDE cronológico); las respuestas siempre cronológicas. Ya viene ordenado del backend.
  - Cada comentario: autor (id, username), `content`, `score`, `myVote`, fechas, `depth`.
- **Comentar / responder** → `POST /posts/{postId}/comments { content, parentCommentId? }`.
- **Votar comentario** → `PUT/DELETE /comments/{commentId}/vote`.
- **Editar** (autor) → `PATCH /comments/{id} { content }`.
- **Moderar / borrar** → `PATCH /comments/{id}/status`.

### Perfil de usuario (escenario 4)
- `GET /users/{id}` (`UserResponse`): username, bio, rol, estado, `followerCount`, `followingCount`, `followedByMe`, fecha. (Sin email en perfil público.)
- **Publicaciones del usuario** → `GET /posts?authorId={id}`.
- **Seguir / dejar de seguir** → `POST` / `DELETE /users/{id}/follow` (idempotentes, 204). Usar `followedByMe` para el estado del botón. Solo se puede seguir a CONTRIBUTOR+ (config) → si no, 400.
- **Seguidores / seguidos** → `GET /users/{id}/followers` y `/following` (paginados).

### Perfil propio
- `GET /users/me` (incluye email) y `PATCH /users/me { bio }`.
- Mostrar aviso si `status = SUSPENDED` (solo lectura).

### Panel de moderación (rol MODERATOR+)
- Ver contenido en cualquier estado (usar `?status=` en `GET /posts`, y los comentarios ocultos ya vienen con contenido visible para moderadores).
- Acciones de estado sobre posts y comentarios (HIDDEN / ACTIVE / DELETED_BY_MOD).

### Panel de administración (rol ADMIN)
- **Usuarios** → `GET /admin/users` (paginado, filtros `?role= &status=`), cambiar rol `PATCH /admin/users/{id}/role`, cambiar estado `PATCH /admin/users/{id}/status`. (Un admin no puede cambiarse a sí mismo → 409.)
- **Categorías** → `GET /categories`, `POST /categories { name, description }`.

## 4. Componentes reutilizables candidatos

- Cliente HTTP con interceptor de auth (Basic hoy, JWT mañana) y manejo de `ProblemDetail`.
- `<MarkdownContent>` (render + sanitize).
- `<VoteControl>` (score + up/down, optimista, deshabilitado en contenido propio).
- `<PostCard>` / `<PostList>` con paginación.
- `<CommentTree>` (arma jerarquía desde la lista plana con `depth`/`parentCommentId`).
- `<ImageUploader>` (multipart, límites, preview) y `<PostGallery>`.
- `<CategoryPicker>` (1–3).
- `<RoleGate>` / `usePermissions` (muestra acciones según rol, pero tolera 403 del backend).
- `<StatusBadge>` para estados de contenido y `<UserBadge>` para rol.
- Manejo de estados vacíos, carga y error consistente.

## 5. Dependencias / cambios que puede necesitar el backend

- **CORS** para el origen del front (o proxy de Vite en dev). Hoy no está configurado.
- (Opcional, a evaluar en el spec) Endpoint de **"capabilities"** que devuelva las reglas efectivas (`forum.rules`) para que la UI muestre/oculte acciones sin hardcodear ni depender de recibir 403.
- (Futuro) Migración de **HTTP Basic → JWT**; el front debe tener el auth aislado para absorber el cambio.
- (Futuro) Orden de posts por score / "mejores", si se quiere en el feed.

## 6. Preguntas abiertas para el spec del front

- ¿SPA sola contra el backend, o SSR? (Para MVP, SPA alcanza.)
- ¿Cómo se guardan las credenciales Basic (memoria vs sessionStorage) y UX de "sesión"?
- ¿Se sirve el front como estático aparte, detrás del mismo dominio (proxy) o distinto origen (CORS)?
- ¿Qué librería de UI/estilos?
- ¿Se agrega el servicio del front al `docker-compose.yml` (monorepo) para levantar todo junto?
- ¿Nivel de i18n? (por ahora todo en español).
