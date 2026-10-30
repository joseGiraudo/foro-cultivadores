# Decisiones

Registro de decisiones del MVP: **técnicas** (cómo se implementó) y **de negocio** (cómo se
interpretaron o extendieron las reglas del spec). Formato de las técnicas: fecha · decisión · motivo.

> **Alcance:** esto cubre el **MVP** (sept 2026). Las decisiones de la etapa posterior (auth por
> cookie, email/Resend, consentimiento legal, R2, hardening, bootstrap de prod) están documentadas en
> [`produccion-pendientes.md`](produccion-pendientes.md).

## Técnicas — backend

- 2026-09-25 · Spring Boot 4.1.1 (última GA, sobre Spring Framework 7) con starters modulares (`spring-boot-starter-webmvc`, etc.) · El spec pide "última versión estable de Spring Boot".
- 2026-09-25 · JDK: Temurin 21 ya instalado en la máquina (`~/.jdks/temurin-21.0.10`) · Cumple "Java 21 o superior (LTS)" sin instalar nada nuevo.
- 2026-09-25 · Maven Wrapper (`mvnw`) en lugar de Maven global · Reproducibilidad sin depender de una instalación local de Maven.
- 2026-09-25 · springdoc-openapi 3.1.1 (línea 3.x) · Es la primera línea compatible con Spring Boot 4; las 2.x son solo para Boot 3.
- 2026-09-25 · Postgres 16 tanto en Docker Compose como en Testcontainers · Reproducibilidad: misma versión de motor en local y en tests, en vez de `postgres:latest`.
- 2026-09-25 · Spring Security se incorpora en el Hito 2 (no en el Hito 1) · Mantener el Hito 1 acotado (contexto + Swagger abierto) y agregar auth cuando corresponde según la sección 9.
- 2026-09-25 · La JVM corre en UTC (`TimeZone.setDefault("UTC")` en el `main` y `-Duser.timezone=UTC` en surefire) · Todos los timestamps son `Instant` (UTC); además evita que el driver de Postgres propague el alias legacy `America/Buenos_Aires`, que Postgres 16 rechaza al abrir la conexión.
- 2026-09-25 · Follows: `Follow` guarda `followerId`/`followingId` como `Long` (con FK reales) e inversión de dependencias (`users` define `FollowStats`, `follows` la implementa) para evitar el ciclo `users↔follows`. Los listados de seguidores/seguidos los arma `follows` reusando `UserService.responsesForIds` (sin N+1) · Mismo patrón que votos/imágenes.
- 2026-09-25 · Para el seeder de contenido (`dev`) se hicieron públicos `PostVoteService`, `CommentVoteService`, `ImageService` y `FollowService`, y `PostResponse`/`CommentResponse`; el seeder usa los mismos services que la API construyendo un `AuthUser` por usuario · Cumple "las imágenes de ejemplo pasan por el mismo ImageStorage" y respeta todas las reglas al sembrar.
- 2026-09-25 · `ImageService` expone una sobrecarga `upload(postId, principal, byte[])` para el seeder (además de la de `MultipartFile`) · Evita depender de `MultipartFile` (spring-test) en el código de producción del seeder.
- 2026-09-25 · Procesamiento de imágenes: validación por **magic bytes** (jpeg/png/webp) + decodificación con `ImageIO`; re-codificación para eliminar metadatos (EXIF/GPS). Librería: TwelveMonkeys `imageio-webp` 3.15.0 (solo lectura de WebP; JPEG/PNG con el `ImageIO` estándar) · El spec deja la librería a criterio y pide re-codificar para limpiar metadatos.
- 2026-09-25 · **WebP se re-codifica a PNG** (sin pérdida) al subir, porque no hay codificador WebP en Java puro; su `contentType` almacenado queda `image/png` · Se acepta subir WebP (validado), pero se guarda/sirve como PNG. Si a futuro se quiere preservar WebP, hará falta una librería nativa (p.ej. binding a libwebp).
- 2026-09-25 · `PostImage` guarda `postId` como `Long` (con FK real) y se usa inversión de dependencias (`posts` define `PostImages`, `images` la implementa) para evitar el ciclo `posts↔images` · Mismo patrón que votos.
- 2026-09-25 · El directorio `base-path` se crea y valida (escritura) al arrancar, en `LocalDiskImageStorage` (@PostConstruct); si no se puede, la app falla · Requisito de la sección 5 (diferido desde el Hito 3). En tests se usa un directorio temporal vía `@DynamicPropertySource`; `data/` está en `.gitignore`.
- 2026-09-25 · Límite de tamaño: multipart de Spring en 5MB (request 6MB) coherente con `forum.images.max-file-size`; `MaxUploadSizeExceededException` → 413, y el service también valida el tamaño (para el caso de MockMvc, que no aplica el límite del contenedor) · Doble red de seguridad para el 413.
- 2026-09-25 · Para evitar ciclos `posts↔votes` y `comments↔votes`, se usa inversión de dependencias: `posts` define `PostVoteStats`/`PostCommentStats`, `comments` define `CommentVoteStats`, y las implementaciones viven en `votes` (y `comments` para el conteo). Así `posts` no depende de `votes`/`comments` y `comments` no depende de `votes` · Mantiene el grafo de módulos acíclico; Spring inyecta las implementaciones.
- 2026-09-25 · `PostVote`/`CommentVote` guardan `userId` y `postId`/`commentId` como `Long` (con FK reales en la migración), sin asociación JPA a `User`/`Post`/`Comment` · Desacopla las entidades de voto y simplifica las consultas agregadas por id; el spec pide "claves foráneas reales", que se cumplen a nivel de tabla.
- 2026-09-25 · score, commentCount y myVote se calculan con consultas agregadas en lote (por conjunto de ids), nunca ítem por ítem · Evita N+1 en listados (requisito del spec).
- 2026-09-25 · `PUT .../vote` responde 200 con `{ score, myVote }` (score actualizado y voto del usuario); `DELETE .../vote` responde 204 siempre (idempotente) · El spec no fija el body del PUT; devolver el score es útil para el cliente.
- 2026-09-25 · El orden de comentarios se implementa completo ya en el Hito 5 pero con un mapa de scores vacío (todos 0), por lo que `SCORE` cae de hecho en cronológico hasta el Hito 6 · El spec dice "orden cronológico por ahora; el orden por score llega en el hito 6"; la estructura queda lista para inyectar scores reales en el Hito 6. `CommentResponse` aún no incluye `score`/`myVote`.
- 2026-09-25 · El hilo de comentarios se arma en `CommentService` como lista plana en recorrido en profundidad; raíces según `comment-order` del tipo de post, respuestas siempre por `createdAt` asc (desempate por id) · Cumple la sección 6 del spec; testeable con un árbol conocido.
- 2026-09-25 · `PostResponse`/`PostSummaryResponse` todavía no incluyen `score`/`commentCount`/`myVote` (Hito 6) ni `images`/`thumbnailUrl` (Hito 7) · El propio spec agrega esos campos en esos hitos; son aditivos y no rompen los tests del Hito 4.
- 2026-09-25 · `content` de Post mapeado como `VARCHAR(20000)` (no `TEXT`) · Coincide con `@Size(max=20000)` y con lo que espera `ddl-auto=validate`; evita fricción de tipos entre `text` y lo que infiere Hibernate.
- 2026-09-25 · Anti N+1 con `@BatchSize`: a nivel de clase en `User` (autor) y en la colección `categories` de Post · En Hibernate 7 `@BatchSize` no es válido sobre un `@ManyToOne` (falla el arranque); se pone en la clase destino.
- 2026-09-25 · Filtros dinámicos de `GET /posts` con `JpaSpecificationExecutor` + `PostSpecifications` · Combina filtros opcionales (categoría/autor/tipo/estado) sin romper la paginación.
- 2026-09-25 · `GlobalExceptionHandler`: se agregaron handlers 400 para cuerpo JSON ilegible (`HttpMessageNotReadableException`) y parámetro con tipo inválido (`MethodArgumentTypeMismatchException`, p.ej. un enum desconocido en query) · Devuelven `ProblemDetail` consistente.
- 2026-09-25 · `PostType` (QUESTION/ARTICLE/GUIDE) vive en `shared` y `CommentOrder` en `rules` · `ForumPolicy` (rules) necesita `PostType` y `posts` necesita `ForumPolicy`; poner `PostType` en `shared` evita el ciclo `posts ↔ rules`.
- 2026-09-25 · `ForumPolicy` valida al arranque (via `InitializingBean`) que estén todos los `PostType` en `post-creation-min-role` y `comment-order` y que los roles mínimos estén definidos; falla con mensaje claro si no · Requisito de la sección 5 del spec.
- 2026-09-25 · La validación/creación del directorio `base-path` de imágenes se hará en el Hito 7 (junto con `LocalDiskImageStorage`), no en el Hito 3 · El path ya se lee y valida como no-vacío en `ForumPolicy`; crear el directorio corresponde al componente de almacenamiento.
- 2026-09-25 · Layout de monorepo: el backend Spring Boot vive en `backend/`; `docker-compose.yml` y `docs/` quedan en la raíz; el frontend React irá en `frontend/` más adelante · Se prevé sumar un frontend; se movió ahora que no hay commits y el backend es chico. Los comandos Maven se corren desde `backend/`.
- 2026-09-25 · En los endpoints paginados se usa `@PageableDefault(size = 20, ...)` explícito · La anotación por defecto usa 20→10 y pisa la propiedad global; el spec pide tamaño por defecto 20 (máximo 100, que sí lo aplica `spring.data.web.pageable.max-page-size`).
- 2026-09-25 · `UserResponse` no incluye todavía `followerCount`/`followingCount`/`followedByMe` · Los follows son del Hito 8; esos campos se agregan ahí (son campos aditivos, no rompen los tests del Hito 2).
- 2026-09-25 · Enums `Role` y `UserStatus` viven en el módulo `users` (parte del modelo de User); otros módulos dependen de `users` para usarlos · Coherente con el modelo de datos del spec; la dependencia entre módulos es de tipos/servicios, no de repositorios.
- 2026-09-25 · Módulo `users` en un único paquete plano con la mayoría de los tipos package-private (repository, DTOs, controllers) · Cumple "lo que no necesita ser público va package-private" sin exponer internals.
- 2026-09-25 · Errores de dominio mediante una única `ApiException` con factorías estáticas (`notFound`, `forbidden`, `conflict`, `badRequest`) traducida a `ProblemDetail` en un `@RestControllerAdvice` · Menos clases que una jerarquía de excepciones, extensible por hito.

## Técnicas — frontend

- 2026-09-26 · Frontend (Hito 1) scaffoldeado con `create-vite` en `frontend/`: React 19, Vite 8, TypeScript 6, oxlint (no eslint) · Son las versiones que trae el scaffold moderno; el `frontend-spec.md` asumía React 18, pero se toma lo que da el generador para no quedar atrás.
- 2026-09-26 · Tailwind CSS v4 vía plugin `@tailwindcss/vite` + `@import 'tailwindcss'` en `index.css`; sin `tailwind.config.js` ni `postcss.config.js` · v4 no los requiere por defecto; menos archivos de config.
- 2026-09-26 · Proxy de Vite: `/api/*` → `http://localhost:8080/*` con `rewrite` que quita `/api` · El front usa rutas relativas `/api/...` y evita configurar CORS en el backend para el MVP.
- 2026-09-26 · Cliente HTTP único (`src/api/client.ts`) que traduce `ProblemDetail` (status, detail, fieldErrors) a `ApiError` · Aísla la auth (migrable sin tocar las vistas) y centraliza el manejo de errores. (La auth pasó después de header Basic a cookie de sesión; ver `produccion-pendientes.md`.)

## De negocio

Decisiones sobre **reglas de negocio**: cómo se interpretaron o extendieron las reglas del spec.

### Hito 2 — Usuarios y auth
- **El email no se devuelve en la respuesta de `POST /auth/register`.** El spec dice "el email solo en `/users/me` y en endpoints de admin"; se aplica al pie de la letra. Quien se registra puede ver su email llamando a `GET /users/me`.
- **Un admin no puede cambiar su propio `status` (ni suspenderse ni reactivarse): devuelve 409.** El spec solo prohíbe "suspenderse a sí mismo"; se extendió a cualquier cambio de estado propio por simplicidad y para evitar estados ambiguos. Si en el futuro se quiere permitir que un admin se reactive a sí mismo, revisar `UserService.changeStatus`.
- **Un usuario suspendido tampoco puede ejecutar acciones de admin** (aunque tenga rol alto): la regla "cualquier escritura de un SUSPENDED devuelve 403" se aplica también a los endpoints de administración.

### Hito 3 — Categorías y reglas
- **La unicidad del nombre de categoría es case-insensitive.** El spec pide "name único"; se interpretó que "Interior" e "interior" son la misma categoría (evita duplicados por mayúsculas). Si se quisiera permitir nombres que solo difieren en mayúsculas, cambiar `CategoryRepository.existsByNameIgnoreCase`.

### Hito 4 — Posts
- **Un `categoryId` inexistente al crear/editar un post devuelve 400** (no 404). Se interpreta como un dato inválido dentro del payload, no como "el recurso post no existe". Ver `CategoryService.getEntitiesByIds`.
- **Si un usuario no-moderador manda `?status=` en `GET /posts`, el parámetro se ignora** y solo ve posts `ACTIVE` (en vez de devolver 403/400). El spec marca ese filtro como "solo mods"; se optó por ignorarlo silenciosamente para no romper clientes. Ver `PostService.list`.
- **`updatedAt` se actualiza también en los cambios de estado** (ocultar/eliminar/restaurar), no solo al editar título/contenido/categorías. Se consideró que un cambio de estado es una modificación del post. Si se quisiera reservar `updatedAt` solo para ediciones de contenido, revisar `Post.changeStatus`.

### Hito 5 — Comentarios
- **Comentar un post visible pero no ACTIVE devuelve 409** (caso de un moderador sobre un post `HIDDEN`); para un usuario común el post no es visible y devuelve 404. Solo se comenta en posts `ACTIVE`. Ver `CommentService.create`.
- **Un `parentCommentId` inválido (inexistente, de otro post, o no ACTIVE) devuelve 400.** Es dato del payload; sigue la sección 6 del spec.
- **La edición y el cambio de estado de un comentario son a nivel comentario** (`/comments/{id}`), sin volver a chequear la visibilidad del post: el autor/moderador puede gestionar el comentario aunque el post haya cambiado de estado. Si se quisiera acoplar a la visibilidad del post, revisar `CommentService.update`/`changeStatus`.

### Hito 6 — Votos
- **`commentCount` cuenta solo comentarios `ACTIVE`.** El spec no lo define; se optó por contar los comentarios activos (no los ocultos/eliminados). Si se quisiera contar todos, revisar `PostCommentStatsImpl` / `CommentRepository.countByPostIdsAndStatus`.
- **Votar (PUT) un post/comentario visible pero no ACTIVE devuelve 409** (caso de un moderador sobre contenido `HIDDEN`); para un usuario común no es visible y devuelve 404. Solo se vota contenido `ACTIVE`.
- **`DELETE .../vote` no valida rol ni existencia del contenido**: es idempotente (204 siempre) y solo requiere estar autenticado y no suspendido. Quitar el propio voto se considera limpieza, no una escritura sujeta a `*-vote-min-role`.

### Hito 7 — Imágenes
- **Una imagen WebP subida se guarda y se sirve como PNG** (se re-codifica para limpiar metadatos y no hay codificador WebP en Java puro). Es decir, se acepta el upload de WebP pero su `contentType` pasa a `image/png`. Ver `ImageProcessor`. Detalle técnico en la sección Técnicas.
- **La subida/borrado de imágenes es solo del autor** (ni siquiera un moderador puede subir a un post ajeno), y solo si el post está `ACTIVE`. Post no visible → 404; post visible-pero-no-ACTIVE (moderador) → 409.
- **`position` de las imágenes arranca en 0** (máximo actual + 1, con máximo inicial −1). La miniatura es la imagen de menor `position`.
- **`sizeBytes` es el tamaño después de procesar** (re-codificado), no el del archivo subido, tal como indica el spec.

### Hito 8 — Follows
- **`followedByMe` es `null` para usuarios anónimos**; para un usuario autenticado es `true`/`false`. En el propio perfil (`/users/me`) queda `false` (no te seguís a vos mismo). Ver `UserService.toResponse`.
- **Seguirse a uno mismo → 400**; seguir a un usuario con rol menor a `follow-target-min-role` → 400; seguir a un usuario inexistente → 404. `POST` es idempotente (204 aunque ya lo siga) y `DELETE` también (204 aunque no lo siga).
- **Los contadores de follows en `UserResponse` cuentan todas las relaciones** (no dependen del estado del usuario seguido/seguidor). En el MVP no hay estado "borrado" de follows: se crean y se eliminan físicamente.

## Preguntas abiertas

Nada pendiente del MVP. Las funcionalidades que estaban "fuera del MVP" (búsqueda, feed de seguidos,
notificaciones, cuentas de negocio, JWT, etc.) ya se fueron implementando en la etapa post-MVP; lo que
falta para salir a producción está en [`produccion-pendientes.md`](produccion-pendientes.md) y las
ideas de mejora en [`mejoras-futuras.md`](mejoras-futuras.md).
