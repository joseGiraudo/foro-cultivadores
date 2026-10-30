# Arquitectura y convenciones

Cómo está construido el proyecto hoy. El "por qué" puntual de cada decisión está en
[`decisiones.md`](decisiones.md); el detalle original del MVP, en [`mvp-spec.md`](mvp-spec.md).

## Monolito modular (paquete por funcionalidad)

El backend se organiza en `com.forum` con un paquete por módulo. Cada módulo tiene su controller,
service, repository, entidades y DTOs; lo que no necesita ser público va package-private, y un módulo
no toca el repository de otro (usa su service).

```
com.forum
├── shared/              # errores (ProblemDetail), seguridad, tipos comunes (PostType, VoteValue)
├── rules/               # ForumPolicy: reglas de negocio configurables
├── users/               # usuarios, roles, auth, perfil, imágenes de usuario, bootstrap de admin
├── follows/             # seguidores
├── categories/          # categorías (+ seeders dev y prod)
├── posts/               # publicaciones
├── images/              # PostImage + ImageStorage (disco local / R2) + procesamiento
├── comments/            # comentarios anidados, fijado
├── votes/               # votos de posts y comentarios
├── saves/               # guardados (bookmarks)
├── notifications/       # notificaciones in-app
├── contributorrequests/ # peticiones para ser growshop
├── email/               # EmailSender (logging en dev / Resend en prod)
└── bootstrap/           # config del arranque de producción
```

## Inversión de dependencias (grafo de módulos acíclico)

Para evitar ciclos entre módulos, lo que un módulo necesita de otro se expresa como **interfaz en el
módulo que la consume**, implementada en el módulo que tiene los datos. Spring inyecta la
implementación. Ejemplos:

- `posts` define `PostVoteStats` / `PostCommentStats` / `PostSaveStats` / `PostImages` /
  `FollowedAuthors`; los implementan `votes` / `comments` / `saves` / `images` / `follows`.
- `notifications` implementa `CommentNotifier` (de `comments`) y `ContributorRequestNotifier`
  (de `contributorrequests`).
- `images` implementa `PostImageWriter` (de `posts`) para subir imágenes sin generar un ciclo.

Así `posts` no depende de `votes`/`comments`/etc. y el grafo queda acíclico.

## Convenciones

- **Nunca se exponen entidades JPA**: DTOs de request/response como `records`.
- El autor de cualquier contenido sale del **usuario autenticado**, nunca del body.
- Errores en **`ProblemDetail`** (RFC 9457) vía `GlobalExceptionHandler` + `ApiException` con
  factorías (`notFound`/`forbidden`/`conflict`/…). La validación de Bean Validation da 400 con errores
  por campo.
- Paginación con `Pageable` (default 20, máx 100, orden `createdAt` desc).
- Anti N+1: score, conteos (comentarios, follows, guardados) y miniaturas se calculan en lote, no ítem
  por ítem.
- Migraciones con **Flyway** (`V1..Vnn`), `ddl-auto=validate`. Enums persistidos como `STRING`.
  La JVM corre en **UTC**.

## Reglas de negocio configurables

Toda regla "ajustable" vive en **`rules/ForumPolicy`** (`@ConfigurationProperties`, prefijo
`forum.rules`), que se **valida al arrancar** (falla con mensaje claro si falta algo). Ningún rol
mínimo se hardcodea en controllers/services (excepción: los endpoints de admin, fijos en `ADMIN`).
Ejemplos: `post-creation-min-role` por tipo, `comment-order`, `follow-target-min-role`,
`business-profile-min-role`, `cover-image-min-role`. Cambiar el `application.yml` cambia el
comportamiento sin tocar código.

**El front no replica estas reglas**: intenta la acción y maneja el 403 con elegancia (el rol solo se
usa para ordenar la navegación, es cosmético).

## Autenticación y seguridad

- **Sesión por cookie httpOnly** (`SameSite=Strict`, `Secure` en prod). Login en `POST /auth/login`,
  logout en `/auth/logout`; el `SecurityContext` se guarda en la sesión. Contraseñas BCrypt; el rol y
  el estado se leen de la base al autenticar. (HTTP Basic quedó solo para los tests, vía un flag.)
- CSRF cubierto por la cookie `SameSite=Strict`, que depende de que el front se sirva en el **mismo
  origen** que la API (proxy de Vite en dev; reverse proxy en prod).
- **Verificación de email** obligatoria para iniciar sesión; recuperación de contraseña por token
  (se guarda el hash SHA-256, nunca el token en claro).
- **Hardening**: rate-limiting por IP en `/auth` (429 + Retry-After), headers de seguridad (HSTS,
  Referrer-Policy, anti-clickjacking), Swagger apagable en prod, y lectura de IP/esquema reales detrás
  del proxy (`forward-headers`). Detalle y variables en [`produccion-pendientes.md`](produccion-pendientes.md).

## Almacenamiento de imágenes

Interfaz **`ImageStorage`** (`store`/`load`/`delete`); el resto del código depende solo de ella. La
implementación se elige por config en `ImageStorageConfig`:

- **`LocalDiskImageStorage`** (default dev): disco local (`forum.storage.local.base-path`).
- **`R2ImageStorage`** (prod, `R2_ENABLED=true`): Cloudflare R2 (S3-compatible, AWS SDK v2, path-style).

Las imágenes se validan por **contenido real** (magic bytes) y se **re-codifican** para borrar EXIF/GPS
(`ImageProcessor`); el WebP se normaliza a PNG. Se sirven por el backend (`GET /images/{id}`, con la
misma visibilidad que el post), así que las URLs no cambian según el storage.

## Email

Interfaz **`EmailSender`** elegida por config en `EmailConfig`: **`LoggingEmailSender`** en dev (loguea
el link por consola) y **`ResendEmailSender`** en prod (cuando hay `FORUM_EMAIL_RESEND_API_KEY`).

## Frontend

SPA React que consume la API. Todas las llamadas pasan por un **cliente HTTP único**
(`src/api/client.ts`) que antepone `/api`, manda la cookie (`credentials: 'include'`) y traduce
`ProblemDetail` a un error de UI. El Markdown se **renderiza y sanitiza** en el front (react-markdown +
rehype-sanitize, defensa anti-XSS). Estilos con Tailwind v4 y componentes propios (paleta "orgánica",
tonos cálidos). Estado del servidor con TanStack Query (cache, invalidación, optimismo en votos/follows).
