# Foro de Cultivadores — Spec del MVP

> ⚠️ **Documento histórico.** Es el spec original del MVP (sept 2026) y **no refleja el estado actual**
> del proyecto: la auth pasó de HTTP Basic a cookie de sesión, y se sumaron muchas features post-MVP
> (búsqueda, notificaciones, guardados, growshops, verificación de email, etc.). Para el estado actual
> ver [`README.md`](README.md) y [`arquitectura.md`](arquitectura.md). Se conserva como referencia del
> diseño base y del modelo de datos original.

> Documento de referencia para desarrollar el MVP con Claude Code.
> El diseño a largo plazo está en `forum-project-recap.md`. Este spec define **solo el MVP** y, si hay contradicciones con el recap, **manda este spec** (por ejemplo, `VERIFIED_USER` ya no existe).

## 0. Instrucciones para Claude Code

- Trabajá **un hito por vez** (sección 9). Al terminar cada uno: tests en verde, resumen de lo hecho, y **pará** para que lo revise antes de seguir.
- **No agregues funcionalidades fuera de este spec**, aunque parezcan obvias. Si algo falta o es ambiguo, preguntá. No inventes reglas de negocio.
- Si tomás una decisión técnica no cubierta acá, registrala en `docs/decisions.md` (una línea: fecha, decisión, motivo).
- Identificadores de código en inglés. Comentarios y documentación pueden ir en español.

## 1. Objetivo del MVP

Tener un foro funcional mínimo para **probar manualmente las reglas de negocio y los flujos principales** antes de construir el resto:

1. Un usuario publica una consulta con una foto de su planta; otros responden y las mejores respuestas, por votos, aparecen primero.
2. Un grow publica un artículo (por ejemplo, sobre una genética nueva) con párrafos e imágenes; la gente lo vota.
3. Un grow publica una guía para principiantes; la gente la vota y comenta con dudas.
4. Un usuario entra al perfil de un grow, ve sus publicaciones y lo sigue.

El MVP se prueba manualmente desde **Swagger UI**. No hay frontend en esta etapa.

### Incluido
- Registro y autenticación
- Usuarios con roles y estado
- Categorías
- Posts con tipos (`QUESTION`, `ARTICLE`, `GUIDE`) y contenido en Markdown
- Imágenes en posts (almacenamiento en disco local)
- Comentarios anidados
- Votos en posts y en comentarios
- Follows entre usuarios (sin feed)
- Moderación básica (ocultar/eliminar contenido, suspender usuarios, cambiar roles)

### Fuera del MVP (no implementar)
- Catálogos (`CATALOG`); probablemente sea una entidad propia en el futuro
- Solicitudes de verificación (el admin cambia roles directamente)
- Cuentas de negocio (`accountType`, `BusinessProfile`)
- Feed de seguidos y notificaciones
- Denuncias/reportes
- Búsqueda de texto
- Almacenamiento en la nube (S3 o similar)
- Restricciones legales o de edad
- JWT / refresh tokens, emails, eventos asíncronos

## 2. Stack

- Java 21 o superior (LTS), última versión estable de Spring Boot
- Spring Web, Spring Data JPA, Spring Security, Bean Validation
- PostgreSQL (vía Docker Compose en local)
- Flyway para migraciones (`spring.jpa.hibernate.ddl-auto=validate`)
- springdoc-openapi (Swagger UI en `/swagger-ui.html`)
- Tests: JUnit 5, MockMvc, Testcontainers (Postgres real, **no H2**)
- Build: Maven
- Procesamiento de imágenes: la librería queda a criterio (por ejemplo `ImageIO` + TwelveMonkeys para WebP). Registrar la elección en `docs/decisions.md`.

## 3. Arquitectura y convenciones

### Paquetes por funcionalidad (monolito modular)

```
com.forum
├── shared/          # errores, config de seguridad, utilidades comunes
├── users/
├── follows/
├── categories/
├── posts/
├── images/          # PostImage, interfaz ImageStorage y su implementación local
├── comments/
├── votes/           # votos en posts y en comentarios
└── rules/           # políticas de negocio configurables (ver sección 5)
```

Cada módulo contiene su controller, service, repository, entidades y DTOs. Lo que no necesita ser público va package-private. Un módulo no accede al repository de otro; usa su service.

### Convenciones obligatorias
- **Nunca exponer entidades JPA** en la API. Usar DTOs de request y de response separados (records de Java).
- El autor de cualquier contenido sale del usuario autenticado, **nunca del body**.
- Enums persistidos con `@Enumerated(EnumType.STRING)`.
- `spring.jpa.open-in-view=false`.
- Errores con `ProblemDetail` (RFC 9457) mediante un `@RestControllerAdvice` global:
  - `400` validación (incluir errores por campo)
  - `401` no autenticado
  - `403` sin permiso (incluye usuario suspendido intentando escribir)
  - `404` no existe o no es visible para quien consulta
  - `409` conflicto (username/email duplicado, transición de estado inválida)
  - `413` archivo demasiado grande
  - `415` tipo de archivo no permitido
- Paginación con `Pageable`: tamaño por defecto 20, máximo 100, orden por defecto `createdAt` descendente.
- Evitar N+1 en listados: score, cantidad de comentarios, cantidad de seguidores y miniatura se calculan con consultas agregadas o en lote, no ítem por ítem.

### Autenticación (temporal para el MVP)
- **HTTP Basic** con contraseñas en BCrypt, stateless.
- Motivo: es mínima y el rol se lee de la base en cada request, así que los cambios de rol o estado aplican al instante. Se reemplazará más adelante; mantenerla aislada en la configuración de seguridad.
- Configurar Swagger UI para poder autenticarse con Basic.
- Lectura pública: los endpoints `GET` de posts, comentarios, imágenes, categorías, perfiles y seguidores no requieren autenticación.

### Almacenamiento de imágenes
- Interfaz `ImageStorage` (por ejemplo `store`, `load`, `delete`) con una única implementación `LocalDiskImageStorage` que escribe en un directorio configurable.
- El resto del código depende solo de la interfaz, para poder cambiar a almacenamiento en la nube sin tocar services ni controllers.
- El nombre del archivo en disco se genera (UUID). Nunca se usa el nombre enviado por el cliente.

## 4. Modelo de datos

### User
| Campo | Tipo | Notas |
|---|---|---|
| id | Long | |
| username | String | único, 3–30 caracteres, `[a-zA-Z0-9_]` |
| email | String | único, formato válido |
| password | String | BCrypt, mínimo 8 caracteres en el registro |
| bio | String | opcional, máximo 500 |
| role | enum | `USER`, `CONTRIBUTOR`, `MODERATOR`, `ADMIN` |
| status | enum | `ACTIVE`, `SUSPENDED` |
| createdAt | Instant | |

`Role` es jerárquico en el orden listado. Implementar `role.isAtLeast(otherRole)`.

### Category
| Campo | Tipo | Notas |
|---|---|---|
| id | Long | |
| name | String | único, máximo 50 |
| description | String | opcional, máximo 300 |

### Post
| Campo | Tipo | Notas |
|---|---|---|
| id | Long | |
| title | String | 5–150 caracteres |
| content | String (text) | Markdown, 1–20000 caracteres |
| author | User | |
| postType | enum | `QUESTION`, `ARTICLE`, `GUIDE` |
| status | enum | `ACTIVE`, `HIDDEN`, `DELETED_BY_USER`, `DELETED_BY_MOD` |
| categories | Set&lt;Category&gt; | muchos a muchos, entre 1 y 3 |
| createdAt, updatedAt | Instant | |

El backend guarda el Markdown tal cual, sin renderizarlo. Renderizar y sanitizar el HTML es responsabilidad del frontend futuro.

### PostImage
| Campo | Tipo | Notas |
|---|---|---|
| id | Long | |
| post | Post | |
| storageKey | String | nombre generado en disco (UUID + extensión) |
| contentType | String | `image/jpeg`, `image/png` o `image/webp` |
| sizeBytes | Long | tamaño después de procesar |
| position | int | orden dentro del post, asignado automáticamente (máximo actual + 1) |
| createdAt | Instant | |

### Comment
| Campo | Tipo | Notas |
|---|---|---|
| id | Long | |
| content | String | 1–5000 caracteres |
| author | User | |
| post | Post | |
| parentComment | Comment | nullable, para respuestas |
| status | enum | `ACTIVE`, `HIDDEN`, `DELETED_BY_USER`, `DELETED_BY_MOD` |
| createdAt, updatedAt | Instant | |

### PostVote
| Campo | Tipo | Notas |
|---|---|---|
| id | Long | |
| user | User | |
| post | Post | |
| value | enum `VoteValue` | `UPVOTE`, `DOWNVOTE` |
| createdAt, updatedAt | Instant | |

Restricción única `(user_id, post_id)`. Cambiar el voto actualiza la fila existente.

### CommentVote
| Campo | Tipo | Notas |
|---|---|---|
| id | Long | |
| user | User | |
| comment | Comment | |
| value | enum `VoteValue` | el mismo enum que `PostVote` |
| createdAt, updatedAt | Instant | |

Restricción única `(user_id, comment_id)`. Cambiar el voto actualiza la fila existente. Tabla separada de `PostVote` para mantener claves foráneas reales.

### Follow
| Campo | Tipo | Notas |
|---|---|---|
| id | Long | |
| follower | User | quien sigue |
| following | User | a quien se sigue |
| createdAt | Instant | |

Restricción única `(follower_id, following_id)`. Relación en un solo sentido (no mutua).

## 5. Reglas de negocio configurables

Toda regla que queramos experimentar vive en **una sola clase de política** (`rules/ForumPolicy`) que lee de `application.yml` mediante `@ConfigurationProperties`. Los services consultan a esta clase; **ningún rol mínimo se hardcodea en controllers ni services**. La única excepción son los endpoints de administración (roles, estados de usuario, categorías), que quedan fijos en `ADMIN`.

```yaml
forum:
  rules:
    post-creation-min-role:
      QUESTION: USER
      ARTICLE: CONTRIBUTOR
      GUIDE: CONTRIBUTOR
    comment-order:            # SCORE o CHRONOLOGICAL
      QUESTION: SCORE
      ARTICLE: CHRONOLOGICAL
      GUIDE: CHRONOLOGICAL
    comment-min-role: USER
    post-vote-min-role: USER
    comment-vote-min-role: USER
    allow-self-vote: false
    follow-min-role: USER
    follow-target-min-role: CONTRIBUTOR   # a quién se puede seguir
    moderation-min-role: MODERATOR
  images:
    max-per-post: 10
    max-file-size: 5MB
    allowed-types: [image/jpeg, image/png, image/webp]
  storage:
    local:
      base-path: ./data/uploads
```

Si falta la configuración de `post-creation-min-role` o de `comment-order` para algún `PostType`, la aplicación debe **fallar al arrancar** con un mensaje claro. Lo mismo si `base-path` no existe y no se puede crear.

Los límites de multipart de Spring (`spring.servlet.multipart.*`) deben ser coherentes con `forum.images.max-file-size`.

## 6. Reglas de negocio fijas del MVP

### Usuarios
- El registro crea un usuario con `role=USER` y `status=ACTIVE`.
- Un usuario `SUSPENDED` puede autenticarse y leer, pero **cualquier escritura devuelve 403**.
- Solo `ADMIN` puede cambiar roles y estados de otros usuarios. Un admin no puede cambiar su propio rol ni suspenderse a sí mismo (409).

### Posts
- Crear: según `post-creation-min-role` para el tipo elegido.
- Editar (`title`, `content`, `categories`): solo el autor, solo si el post está `ACTIVE`. El `postType` no se puede cambiar.
- Visibilidad:
  - Usuarios comunes y anónimos solo ven posts `ACTIVE`. Un post en otro estado devuelve 404.
  - Usuarios con `moderation-min-role` o superior ven todos los posts y pueden filtrar por `?status=`.

### Transiciones de estado (posts y comentarios, mismas reglas)
| Quién | Desde | Hacia |
|---|---|---|
| Autor | `ACTIVE` | `DELETED_BY_USER` |
| `moderation-min-role`+ | `ACTIVE` | `HIDDEN`, `DELETED_BY_MOD` |
| `moderation-min-role`+ | `HIDDEN` | `ACTIVE`, `DELETED_BY_MOD` |

Los estados `DELETED_*` son terminales. Cualquier otra transición devuelve 409.

### Imágenes
- Flujo en dos pasos: primero se crea el post y después se le suben imágenes. Para intercalar imágenes en un artículo o guía, el autor sube las imágenes y luego edita el `content` para referenciar la `url` de cada una con sintaxis Markdown (`![descripción](url)`). En una consulta alcanza con subirlas; se muestran en el orden de `position`.
- Subir: solo el autor, solo si el post está `ACTIVE`.
- Límites: hasta `max-per-post` imágenes por post (si se supera, 409) y hasta `max-file-size` por archivo (si se supera, 413).
- El tipo se valida por el **contenido real del archivo** (magic bytes), no por la extensión ni por el `Content-Type` enviado. Si no es un tipo permitido o no se puede decodificar, 415.
- **Se eliminan los metadatos** (EXIF, incluida la ubicación GPS) re-codificando la imagen antes de guardarla. Un test debe verificarlo.
- Si falla el guardado en base después de escribir el archivo, se borra el archivo (no dejar huérfanos por errores).
- Eliminar: solo el autor, solo si el post está `ACTIVE`. Borra la fila y el archivo (borrado físico; las imágenes no usan soft delete).
- Servir: `GET /images/{imageId}` devuelve el archivo con su `Content-Type`. Aplica la misma visibilidad que el post: si quien consulta no puede ver el post, 404.
- Si un post pasa a `DELETED_*` o `HIDDEN`, sus imágenes quedan en disco; solo cambia su visibilidad.

### Comentarios
- Crear: según `comment-min-role`, solo en posts `ACTIVE`.
- `parentCommentId` es opcional. Si se envía, el comentario padre debe pertenecer al mismo post y estar `ACTIVE`; si no, 400.
- Sin límite de profundidad en el MVP.
- Editar: solo el autor, solo si está `ACTIVE`.
- Al listar, los comentarios no `ACTIVE` **se incluyen igual** para no romper el hilo, pero con `content: null` y su `status` visible. Los moderadores ven el contenido.
- **Orden al listar** (sin paginación en el MVP):
  - Los comentarios de primer nivel se ordenan según `comment-order` del tipo de post:
    - `SCORE`: score descendente y, en empate, `createdAt` ascendente.
    - `CHRONOLOGICAL`: `createdAt` ascendente.
  - Las respuestas (cualquier nivel por debajo del primero) siempre se ordenan por `createdAt` ascendente.
  - La respuesta es una **lista plana en recorrido en profundidad**: cada comentario va seguido de todas sus respuestas antes de pasar a su siguiente hermano. Cada ítem incluye `parentCommentId` y `depth` (0 para primer nivel).

### Votos en posts
- Votar: según `post-vote-min-role`, solo en posts `ACTIVE`.
- Si `allow-self-vote` es `false`, votar un post propio devuelve 403.
- `score` = cantidad de upvotes − cantidad de downvotes.

### Votos en comentarios
- Votar: según `comment-vote-min-role`, solo en comentarios `ACTIVE` de posts `ACTIVE`.
- Si `allow-self-vote` es `false`, votar un comentario propio devuelve 403.
- `score` se calcula igual que en posts. Los comentarios no `ACTIVE` conservan su score (no se borran votos).

### Follows
- Seguir: según `follow-min-role`.
- Solo se puede seguir a usuarios con rol `follow-target-min-role` o superior; si no, 400.
- Seguirse a uno mismo devuelve 400.
- `POST` es idempotente: si ya lo sigue, devuelve 204 sin cambios. `DELETE` también: 204 aunque no lo siga.
- En el MVP seguir no genera feed ni notificaciones; solo se refleja en contadores y listados.

## 7. Endpoints

**Autenticación:** 🌐 público · 🔑 autenticado · 🛡️ `moderation-min-role`+ · 👑 `ADMIN`

### Auth y usuarios
```
POST   /auth/register                 🌐  { username, email, password }
GET    /users/me                      🔑  perfil propio (útil para verificar login)
PATCH  /users/me                      🔑  { bio }
GET    /users/{id}                    🌐  perfil público (sin email)
```

Las publicaciones de un usuario se listan con `GET /posts?authorId={id}`.

### Follows
```
POST   /users/{id}/follow             🔑  idempotente
DELETE /users/{id}/follow             🔑  idempotente: 204 aunque no exista
GET    /users/{id}/followers          🌐  paginado
GET    /users/{id}/following          🌐  paginado
```

### Administración
```
PATCH  /admin/users/{id}/role         👑  { role }
PATCH  /admin/users/{id}/status       👑  { status }
GET    /admin/users                   👑  paginado, ?role= ?status=
```

### Categorías
```
GET    /categories                    🌐
POST   /categories                    👑  { name, description }
```

### Posts
```
POST   /posts                         🔑  { title, content, postType, categoryIds }
GET    /posts                         🌐  ?categoryId= ?authorId= ?postType= ?status= (solo mods), paginado
GET    /posts/{id}                    🌐
PATCH  /posts/{id}                    🔑  { title?, content?, categoryIds? }
PATCH  /posts/{id}/status             🔑  { status }
```

### Imágenes
```
POST   /posts/{postId}/images         🔑  multipart/form-data, campo "file" (una imagen por request)
DELETE /posts/{postId}/images/{imageId}  🔑
GET    /images/{imageId}              🌐  devuelve el archivo
```

### Comentarios
```
POST   /posts/{postId}/comments       🔑  { content, parentCommentId? }
GET    /posts/{postId}/comments       🌐  lista plana en recorrido en profundidad (ver sección 6)
PATCH  /comments/{id}                 🔑  { content }
PATCH  /comments/{id}/status          🔑  { status }
```

### Votos
```
PUT    /posts/{postId}/vote           🔑  { value }   idempotente: crea o actualiza
DELETE /posts/{postId}/vote           🔑              idempotente: 204 aunque no exista
PUT    /comments/{commentId}/vote     🔑  { value }   idempotente: crea o actualiza
DELETE /comments/{commentId}/vote     🔑              idempotente: 204 aunque no exista
```

### Respuestas principales
- `PostResponse`: id, title, content, postType, status, author (id, username, role), categories, images (id, url, position; ordenadas por position), score, commentCount, myVote (null si anónimo o sin voto), createdAt, updatedAt.
- `PostSummaryResponse` (para listados): igual pero sin `content` (o con un extracto de hasta 200 caracteres) y sin `images`; incluye `thumbnailUrl` (url de la imagen con menor position, null si no tiene).
- `ImageResponse`: id, url (`/images/{id}`), position, contentType, sizeBytes.
- `CommentResponse`: id, content (null si no visible), status, author (id, username), parentCommentId, depth, score, myVote, createdAt, updatedAt.
- `UserResponse`: id, username, bio, role, status, followerCount, followingCount, followedByMe (null si anónimo), createdAt. El email solo en `/users/me` y en endpoints de admin.

## 8. Datos de prueba (perfil `dev`)

Un seeder que corra **solo con el perfil `dev`** (no en migraciones de Flyway) y solo si la base está vacía. Todos los usuarios con contraseña `password123`:

- `admin` (`ADMIN`), `mod` (`MODERATOR`)
- `grow_norte` y `grow_sur` (`CONTRIBUTOR`), con bio de growshop
- `user1`, `user2`, `user3` (`USER`)
- `suspended` (`USER`, `SUSPENDED`)

Categorías: Interior, Exterior, Riego y nutrientes, Plagas y enfermedades, Equipamiento, Genéticas.

Contenido, de forma que cada escenario de la sección 1 se vea funcionando al arrancar:

- Una `QUESTION` de `user1` con una imagen de ejemplo y varias respuestas con distintos scores, más alguna respuesta anidada, para que se note el orden por votos.
- Un `ARTICLE` de `grow_norte` sobre una genética, con imágenes referenciadas en el Markdown y votos positivos y negativos.
- Una `GUIDE` de `grow_sur` para principiantes, con pasos numerados en Markdown y comentarios con dudas en orden cronológico.
- Algunos usuarios siguiendo a `grow_norte` y a `grow_sur`.
- Al menos un post y un comentario en `HIDDEN` para probar la visibilidad.

Las imágenes de ejemplo salen de `src/main/resources/seed-images/` y pasan por el mismo `ImageStorage` que las subidas reales.

## 9. Hitos de desarrollo

Cada hito termina con tests en verde y verificación manual en Swagger.

1. **Setup:** proyecto, Docker Compose con Postgres, Flyway, springdoc, manejo global de errores, test de contexto con Testcontainers.
2. **Usuarios y auth:** entidad User, registro, HTTP Basic, `/users/*`, endpoints de admin, bloqueo de escrituras para `SUSPENDED`, seeder de usuarios.
3. **Categorías y reglas:** entidad Category, endpoints, `ForumPolicy` con validación al arranque, seeder de categorías.
4. **Posts:** CRUD, filtros, visibilidad y transiciones de estado.
5. **Comentarios:** creación con respuestas, listado en profundidad con contenido oculto según estado, edición y transiciones (orden cronológico por ahora; el orden por score llega en el hito 6).
6. **Votos:** votos en posts y en comentarios, score, myVote y commentCount sin N+1, orden de comentarios según `comment-order`.
7. **Imágenes:** `ImageStorage` + implementación local, subida con validación y limpieza de metadatos, borrado, servido con visibilidad, `images` y `thumbnailUrl` en las respuestas.
8. **Follows y seeder completo:** follow/unfollow, listados, contadores y `followedByMe` en perfiles. Completar el seeder con todo el contenido de la sección 8.

## 10. Tests

- Tests de integración (MockMvc + Testcontainers) por endpoint para el camino feliz y los errores principales.
- **Test de matriz de permisos** parametrizado: para cada acción de escritura (incluidas subir/borrar imágenes, votar comentarios y seguir) × cada rol (más anónimo y suspendido), verificar el código esperado. Esta es la red de seguridad para cuando cambiemos las reglas.
- Tests unitarios para `ForumPolicy` y para las transiciones de estado.
- Tests que verifiquen que cambiar la config modifica el comportamiento sin tocar código, por ejemplo:
  - `QUESTION: CONTRIBUTOR` impide que un `USER` pregunte.
  - `comment-order.QUESTION: CHRONOLOGICAL` cambia el orden de las respuestas.
  - `follow-target-min-role: USER` permite seguir a cualquier usuario.
- Imágenes: rechazo por tipo real (un `.jpg` que no es imagen → 415), por tamaño (413) y por cantidad (409); una imagen con EXIF/GPS queda guardada sin metadatos; `GET /images/{id}` de un post `HIDDEN` devuelve 404 a un usuario común. Usar un directorio temporal como `base-path` en los tests.
- Orden de comentarios: un test con un árbol conocido verifica el recorrido en profundidad y el orden por score entre comentarios de primer nivel.

## 11. Definición de terminado (MVP completo)

- `docker compose up` + correr la app con perfil `dev` deja un foro con datos navegables donde se pueden recorrer los cuatro escenarios de la sección 1.
- Todos los endpoints de la sección 7 funcionan y están documentados en Swagger UI (incluida la subida multipart).
- Todos los tests pasan.
- Cambiar cualquier valor de `forum.rules` en `application.yml` modifica el comportamiento sin cambios de código.
