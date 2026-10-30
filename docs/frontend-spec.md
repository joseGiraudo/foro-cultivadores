# Foro de Cultivadores — Spec del Frontend (MVP)

> ⚠️ **Documento histórico.** Es el spec original del front del MVP (sept 2026) y **no refleja el estado
> actual**: el scaffold real fue React 19 (no 18), y la auth pasó de HTTP Basic en sessionStorage a
> cookie de sesión. Para el estado actual ver [`README.md`](README.md) y [`arquitectura.md`](arquitectura.md).
> Se conserva como referencia del diseño base (pantallas, componentes, mapeo a endpoints).

> Documento de referencia para desarrollar el **frontend del MVP** con Claude Code.
> Nace de `ideas-frontend.md` (inventario) y se apoya en el backend ya terminado
> (`mvp-spec.md`, `decisions.md`). Si hay contradicción con el backend, **manda el backend**:
> el front no inventa funcionalidad ni reglas que la API no ofrezca.
>
> Objetivo: una SPA en React que permita **probar manualmente los 4 escenarios del MVP**
> (consulta con foto, artículo, guía, perfil de grow) contra los endpoints existentes.

## 0. Instrucciones para Claude Code

- Trabajá **un hito por vez** (sección 9). Al terminar cada uno: build y lint en verde,
  resumen de lo hecho, y **pará** para que lo revise antes de seguir.
- **No agregues funcionalidades fuera de este spec**, aunque parezcan obvias. Si algo falta
  o es ambiguo, preguntá. No inventes reglas de negocio ni endpoints.
- Si tomás una decisión técnica no cubierta acá, registrala en `docs/decisions.md`
  (una línea: fecha, decisión, motivo).
- Identificadores de código en inglés. Comentarios y textos de UI en español.
- El front vive en `frontend/` (monorepo; el backend está en `backend/`).

## 1. Alcance

Reflejar los 4 escenarios que ya prueba el backend:

1. Un usuario publica una **consulta** (QUESTION) con foto; otros responden y las mejores
   respuestas (por votos) aparecen primero.
2. Un grow publica un **artículo** (ARTICLE) con párrafos e imágenes; la gente lo vota.
3. Un grow publica una **guía** (GUIDE) para principiantes; la gente la vota y comenta.
4. Un usuario entra al **perfil de un grow**, ve sus publicaciones y lo sigue.

Además, por completitud del MVP y porque el backend los ofrece: moderación básica
(rol MODERATOR+) y administración (rol ADMIN: usuarios y categorías).

**Fuera de alcance** (el backend tampoco los tiene): feed de seguidos, notificaciones,
búsqueda, catálogos, denuncias, cuentas de negocio, verificación. El front **no los asume**.

## 2. Decisiones fijas (ya tomadas)

| Tema | Decisión | Nota |
|---|---|---|
| Stack | React 18 + Vite + TypeScript | SPA sola contra el backend. |
| Router | React Router | Rutas en sección 3. |
| Estado del servidor | TanStack Query | Cache, invalidación, estados de carga/error. Encaja con la API REST paginada. |
| Estado de auth | React Context + hook `useAuth` | Sesión Basic; ver sección 5. |
| UI / estilos | **Tailwind CSS** | Componentes propios (sin librería de componentes). |
| Dev / CORS | **Proxy de Vite** | Vite proxea `/api` → backend. **No se toca CORS del backend en el MVP.** |
| Credenciales | **sessionStorage** | Persisten al recargar, se pierden al cerrar la pestaña. |
| Docker | **No** por ahora | Front se corre con `npm run dev`; compose solo levanta Postgres/backend. |
| i18n | Solo español | Sin librería de i18n en el MVP; textos hardcodeados en español. |
| Markdown | `react-markdown` + `rehype-sanitize` | Render **y sanitización** en el front (anti-XSS). |

## 3. Consideraciones técnicas transversales

### Cliente HTTP y base de la API
- Todas las llamadas pasan por **un único cliente HTTP** (`src/api/client.ts`) que:
  - Antepone `/api` a las rutas (el proxy de Vite reescribe `/api` → backend).
  - Inyecta el header `Authorization: Basic ...` si hay sesión (ver sección 5).
  - Parsea respuestas y **traduce `ProblemDetail`** (RFC 9457) a un tipo de error de UI.
- **Aislar el manejo de auth en esta capa**: mañana se migra a JWT cambiando solo acá.
- Configurar el proxy en `vite.config.ts`: `server.proxy['/api'] → http://localhost:8080`
  (con `rewrite` que quite el prefijo `/api`). Anotar el puerto real del backend.

### Manejo de errores (`ProblemDetail`)
La API responde errores con `status`, `detail` y, en validaciones (400), un objeto `errors`
por campo. Un helper central mapea códigos a UX:

| Código | Significado | UX sugerida |
|---|---|---|
| 400 | Validación | Mostrar errores por campo en el form. |
| 401 | No autenticado | Redirigir a login / limpiar sesión. |
| 403 | Sin permiso (incluye suspendido y auto-voto) | Toast "No tenés permiso para esta acción". |
| 404 | No existe o no visible | Pantalla "No encontrado". |
| 409 | Conflicto (duplicado, transición inválida) | Mensaje específico (ej. "Username en uso"). |
| 413 | Archivo muy grande | Mensaje en el uploader. |
| 415 | Tipo de archivo no permitido | Mensaje en el uploader. |

**Principio clave sobre permisos:** la UI muestra/oculta acciones según el rol como
*comodidad*, pero la autorización real la hace el backend. Como los mínimos por acción son
**configurables** (`forum.rules.*`) y hoy **no existe** un endpoint de "capabilities", el front
**no hardcodea las reglas de negocio**: intenta la acción y maneja el 403 con elegancia.
(Sí puede usar el rol para ordenar la navegación —ej. no mostrar el panel de admin a un USER—,
pero eso es cosmético.)

### Paginación
Listados devuelven `Page` de Spring: `content`, `totalElements`, `totalPages`, `number`, `size`.
Tamaño por defecto 20, máximo 100. Params `?page=&size=&sort=`. Componente `<Pagination>` reusable
y helpers en el cliente para consumir `Page<T>`.

### Markdown e imágenes
- El contenido de posts viene en **Markdown crudo**. Renderizar con `react-markdown` +
  `rehype-sanitize` (**obligatorio**, es la defensa anti-XSS del front).
- Las imágenes vienen como URLs relativas `/images/{id}`. Se resuelven contra la base de la API
  (a través del proxy quedan como `/api/images/{id}`; centralizar la resolución en un helper).
- En **artículos/guías** las imágenes van referenciadas dentro del Markdown (`![desc](/images/{id})`).
- En **consultas** se muestran aparte, en galería, ordenadas por `position`.
- **Ojo WebP:** el backend re-codifica WebP a PNG. Usar siempre la `url`/`contentType` que
  devuelve la API, nunca asumir el formato original.

### Roles y estado
Jerarquía `USER < CONTRIBUTOR < MODERATOR < ADMIN`. Un usuario `SUSPENDED` puede leer pero no
escribir (el backend devuelve 403). La UI muestra un aviso persistente si la sesión está suspendida.

## 4. Estructura del proyecto

```
frontend/
├── index.html
├── vite.config.ts            # proxy /api → backend
├── tailwind.config.js
├── tsconfig.json
├── package.json
└── src/
    ├── main.tsx              # bootstrap: Router + QueryClient + AuthProvider
    ├── App.tsx               # layout + rutas
    ├── api/
    │   ├── client.ts         # fetch wrapper: auth header + ProblemDetail
    │   ├── types.ts          # tipos de DTOs de la API (ver sección 7 del mvp-spec)
    │   ├── auth.ts           # register, getMe
    │   ├── posts.ts          # list, get, create, patch, status, vote
    │   ├── comments.ts       # list, create, patch, status, vote
    │   ├── images.ts         # upload, delete, url helper
    │   ├── users.ts          # get, patchMe, follow/unfollow, followers/following
    │   ├── categories.ts     # list, create
    │   └── admin.ts          # users list, role, status
    ├── auth/
    │   ├── AuthContext.tsx   # sesión Basic en sessionStorage
    │   └── useAuth.ts
    ├── components/           # componentes reutilizables (sección 6)
    ├── pages/                # vistas (sección 5)
    ├── hooks/                # useQuery/useMutation por recurso
    └── lib/                  # helpers (markdown, paginación, formato de fechas)
```

## 5. Autenticación y sesión

- **No hay endpoint de login.** El "login" toma usuario/contraseña, arma el header Basic,
  y valida contra `GET /users/me`:
  - 200 → guardar credenciales + perfil en `AuthContext` y en `sessionStorage`.
  - 401 → error "Usuario o contraseña incorrectos".
- **Registro** → `POST /auth/register { username, email, password }`. Tras registrar exitosamente,
  hacer login automático con esas credenciales.
- **Logout** = olvidar credenciales (limpiar `AuthContext` y `sessionStorage`). No hay logout de servidor.
- Al montar la app, si hay credenciales en `sessionStorage`, revalidar con `GET /users/me`
  (si da 401, limpiar sesión).
- **Rutas protegidas:** las acciones de escritura requieren sesión. Un `<RequireAuth>` envuelve
  las rutas que la necesitan y redirige a login conservando el destino.
- Validaciones de formularios **espejo del backend** (feedback rápido; el backend igual valida):
  - username 3–30, `[a-zA-Z0-9_]`; email válido; password ≥ 8.
  - title 5–150; content 1–20000; bio ≤ 500; categorías 1–3.

## 6. Pantallas / vistas → endpoints

Todas las rutas de lectura (`GET`) funcionan sin sesión; las de escritura requieren `<RequireAuth>`.

### Auth
- `/register` — form de registro → `POST /auth/register`. Manejar 409 (username/email en uso).
- `/login` — form usuario/contraseña → validación vía `GET /users/me`.

### Home / listado (`/`)
- Feed general (no personalizado) → `GET /posts` con filtros `?categoryId=&authorId=&postType=&status=`
  (`status` solo visible/aplicable para MODERATOR+) y paginación.
- Tarjeta de post (`PostSummaryResponse`): título, extracto, tipo (badge), autor (username + rol),
  categorías, `thumbnailUrl`, `score`, `commentCount`, `myVote`, fecha.
- Filtros por **categoría** (`GET /categories`) y por **tipo** (QUESTION/ARTICLE/GUIDE).
- Orden por defecto `createdAt` desc (el backend no ordena por score en el feed; no exponerlo).

### Detalle de post (`/posts/:id`)
- `GET /posts/{id}` (`PostResponse`): título, **Markdown renderizado**, tipo, estado, autor,
  categorías, **galería** (por `position`), `score`, `commentCount`, `myVote`, fechas.
- **Votar** → `PUT /posts/{id}/vote { value }` y `DELETE /posts/{id}/vote`. La respuesta del PUT trae
  `{ score, myVote }` para actualizar al instante (voto optimista). Deshabilitar en post propio (auto-voto → 403).
- Sección de comentarios (abajo).
- Acciones del **autor**: editar (`PATCH /posts/{id}`), eliminar
  (`PATCH /posts/{id}/status { DELETED_BY_USER }`), gestionar imágenes.
- Acciones de **moderador**: `PATCH /posts/{id}/status` → HIDDEN / ACTIVE / DELETED_BY_MOD.

### Crear / editar post (`/posts/new`, `/posts/:id/edit`)
- **Crear** → `POST /posts { title, content, postType, categoryIds }` (1–3 categorías).
  - Editor de Markdown con **preview** (reusa `<MarkdownContent>`).
  - Selector de tipo: si el rol no alcanza para ARTICLE/GUIDE, el backend responde 403 → mostrarlo claro.
  - `<CategoryPicker>` multi (1–3).
- **Subir imágenes** (flujo en dos pasos): primero se crea el post, después
  `POST /posts/{postId}/images` (multipart, campo `file`, **una por request**).
  - Para artículos/guías, tras subir, ofrecer **insertar la referencia** `![desc](/images/{id})` en el Markdown.
  - Validar en UI tamaño (≤ 5MB) y tipo (jpeg/png/webp) para feedback; el backend valida por contenido real (413/415).
- **Eliminar imagen** → `DELETE /posts/{postId}/images/{imageId}`.
- **Editar** → `PATCH /posts/{id} { title?, content?, categoryIds? }` (solo autor, solo si ACTIVE; el tipo no cambia).

### Comentarios (dentro del detalle)
- **Listado** → `GET /posts/{postId}/comments`: **lista plana en recorrido en profundidad**;
  cada ítem trae `parentCommentId` y `depth` → el front arma el árbol/indentado.
  Los no visibles vienen con `content: null` y su `status` (mostrar "comentario oculto/eliminado" sin romper el hilo).
  Ya viene ordenado del backend (primer nivel por score en QUESTION, cronológico en ARTICLE/GUIDE; respuestas siempre cronológicas).
- **Comentar / responder** → `POST /posts/{postId}/comments { content, parentCommentId? }`.
- **Votar** → `PUT` / `DELETE /comments/{commentId}/vote`.
- **Editar** (autor) → `PATCH /comments/{id} { content }`.
- **Moderar/borrar** → `PATCH /comments/{id}/status`.

### Perfil público (`/users/:id`)
- `GET /users/{id}` (`UserResponse`): username, bio, rol, estado, `followerCount`,
  `followingCount`, `followedByMe`, fecha. (Sin email.)
- Publicaciones → `GET /posts?authorId={id}`.
- **Seguir / dejar de seguir** → `POST` / `DELETE /users/{id}/follow` (204, idempotentes).
  Usar `followedByMe` para el estado del botón. Solo se puede seguir a CONTRIBUTOR+ (config) → si no, 400 (manejarlo).
- Seguidores / seguidos → `GET /users/{id}/followers` y `/following` (paginados).

### Perfil propio (`/me`)
- `GET /users/me` (incluye email) y `PATCH /users/me { bio }`.
- Aviso si `status = SUSPENDED` (solo lectura).

### Moderación (`/moderation`, rol MODERATOR+)
- Ver contenido en cualquier estado (`?status=` en `GET /posts`; comentarios ocultos ya vienen con contenido).
- Acciones de estado sobre posts y comentarios (HIDDEN / ACTIVE / DELETED_BY_MOD).

### Administración (`/admin`, rol ADMIN)
- **Usuarios** → `GET /admin/users` (paginado, `?role=&status=`), `PATCH /admin/users/{id}/role`,
  `PATCH /admin/users/{id}/status`. (Un admin no puede cambiarse a sí mismo → 409.)
- **Categorías** → `GET /categories`, `POST /categories { name, description }`.

## 7. Componentes reutilizables

- `<MarkdownContent>` — render + sanitize (react-markdown + rehype-sanitize).
- `<VoteControl>` — score + up/down, optimista, deshabilitado en contenido propio; sirve para posts y comentarios.
- `<PostCard>` / `<PostList>` — con `<Pagination>`.
- `<CommentTree>` — arma jerarquía desde la lista plana usando `depth`/`parentCommentId`; maneja comentarios ocultos.
- `<ImageUploader>` (multipart, límites, preview) y `<PostGallery>` (ordenada por `position`).
- `<CategoryPicker>` (1–3).
- `<RoleGate>` / `usePermissions` — muestra acciones según rol, pero tolera 403 del backend (cosmético, no seguridad).
- `<StatusBadge>` (estado de contenido) y `<UserBadge>` (rol) y `<PostTypeBadge>` (Consulta/Artículo/Guía).
- `<RequireAuth>` — protege rutas de escritura.
- Estados de **carga / vacío / error** consistentes (`<Loading>`, `<EmptyState>`, `<ErrorState>`).

## 8. Dependencias del backend (anotadas, no bloqueantes para el MVP)

- **CORS**: no se toca en el MVP porque usamos el proxy de Vite. Si algún día el front corre en
  otro origen sin proxy, habrá que configurar CORS en Spring.
- (Futuro) Endpoint de **"capabilities"** con las reglas efectivas (`forum.rules`) para que la UI
  muestre/oculte acciones sin depender de recibir 403. Hoy no existe; el front maneja el 403.
- (Futuro) Migración **Basic → JWT**: absorbida por la capa de auth aislada (`api/client.ts` + `AuthContext`).
- (Futuro) Orden de posts por score en el feed, si se quiere.

## 9. Hitos de desarrollo

Cada hito termina con build + lint en verde y verificación manual en el navegador
(backend con perfil `dev` corriendo, con datos sembrados).

1. **Setup:** proyecto Vite + TS + Tailwind en `frontend/`, React Router, TanStack Query,
   proxy `/api` en `vite.config.ts`, layout base (navbar + rutas vacías), cliente HTTP con
   parseo de `ProblemDetail`. Verificación: la app levanta y pega un `GET /categories` de prueba.
2. **Auth y sesión:** `AuthContext` (Basic en sessionStorage), páginas `/login` y `/register`,
   revalidación con `GET /users/me`, logout, `<RequireAuth>`, navbar con usuario/rol y aviso de SUSPENDED.
3. **Feed y detalle (lectura):** Home con `<PostList>` + filtros (categoría, tipo) + paginación;
   detalle de post con Markdown renderizado y galería; listado de comentarios con `<CommentTree>`
   (incluye ocultos). Sin escritura todavía.
4. **Votos:** `<VoteControl>` optimista en posts y comentarios; deshabilitado en contenido propio;
   manejo del 403 de auto-voto.
5. **Crear / editar posts + imágenes:** form de creación con editor Markdown + preview + `<CategoryPicker>`;
   `<ImageUploader>` (flujo dos pasos) y borrado de imágenes; inserción de referencias en el Markdown;
   edición y borrado por el autor (`status`).
6. **Comentarios (escritura):** comentar, responder, votar (ya del hito 4), editar por el autor.
7. **Perfiles y follows:** perfil público con publicaciones y botón seguir/dejar de seguir;
   perfil propio con edición de bio; listados de seguidores/seguidos.
8. **Moderación y administración:** panel de moderación (estados de posts/comentarios, ver ocultos);
   panel de admin (usuarios: rol/estado; categorías: crear).

## 10. Definición de terminado (MVP front)

- Con el backend en perfil `dev` (datos sembrados) y `npm run dev` en `frontend/`, se pueden
  recorrer los **4 escenarios** de la sección 1 desde el navegador.
- Todas las pantallas de la sección 6 funcionan contra los endpoints reales.
- El Markdown se renderiza **sanitizado**; las imágenes se ven vía `/images/{id}`.
- Los errores del backend (`ProblemDetail`) se muestran de forma consistente; el 403 por permisos
  no rompe la UI.
- La sesión Basic queda **aislada** en una capa, lista para migrar a JWT sin tocar las vistas.

## 11. Preguntas abiertas (para revisar durante el desarrollo)

- Editor de Markdown: ¿textarea + preview propio, o una librería liviana? (Empezar con textarea + preview.)
- ¿Cuánto invertir en diseño visual del MVP? (Por ahora: funcional y limpio, sin pulido.)
- Manejo de "insertar imagen en el Markdown": ¿botón que copia/inserta la referencia, o solo mostrar la URL a copiar?
