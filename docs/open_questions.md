# Decisiones de reglas de negocio

> Registro de decisiones sobre **reglas de negocio**: las ya tomadas y las que quedan
> pendientes para el futuro. Las decisiones puramente técnicas van en `decisions.md`.

## Decisiones tomadas

### Hito 2 — Usuarios y auth
- **El email no se devuelve en la respuesta de `POST /auth/register`.** El spec dice "el email
  solo en `/users/me` y en endpoints de admin"; se aplica al pie de la letra. Quien se registra
  puede ver su email llamando a `GET /users/me`.
- **Un admin no puede cambiar su propio `status` (ni suspenderse ni reactivarse): devuelve 409.**
  El spec solo prohíbe "suspenderse a sí mismo"; se extendió a cualquier cambio de estado propio
  por simplicidad y para evitar estados ambiguos. Si en el futuro se quiere permitir que un admin
  se reactive a sí mismo, revisar `UserService.changeStatus`.
- **Un usuario suspendido tampoco puede ejecutar acciones de admin** (aunque tenga rol alto): la
  regla "cualquier escritura de un SUSPENDED devuelve 403" se aplica también a los endpoints de
  administración.

### Hito 3 — Categorías y reglas
- **La unicidad del nombre de categoría es case-insensitive.** El spec pide "name único"; se
  interpretó que "Interior" e "interior" son la misma categoría (evita duplicados por mayúsculas).
  Si se quisiera permitir nombres que solo difieren en mayúsculas, cambiar
  `CategoryRepository.existsByNameIgnoreCase`.

### Hito 4 — Posts
- **Un `categoryId` inexistente al crear/editar un post devuelve 400** (no 404). Se interpreta como
  un dato inválido dentro del payload, no como "el recurso post no existe". Ver
  `CategoryService.getEntitiesByIds`.
- **Si un usuario no-moderador manda `?status=` en `GET /posts`, el parámetro se ignora** y solo ve
  posts `ACTIVE` (en vez de devolver 403/400). El spec marca ese filtro como "solo mods"; se optó por
  ignorarlo silenciosamente para no romper clientes. Ver `PostService.list`.
- **`updatedAt` se actualiza también en los cambios de estado** (ocultar/eliminar/restaurar), no solo
  al editar título/contenido/categorías. Se consideró que un cambio de estado es una modificación del
  post. Si se quisiera reservar `updatedAt` solo para ediciones de contenido, revisar `Post.changeStatus`.

### Hito 5 — Comentarios
- **Comentar un post visible pero no ACTIVE devuelve 409** (caso de un moderador sobre un post
  `HIDDEN`); para un usuario común el post no es visible y devuelve 404. Solo se comenta en posts
  `ACTIVE`. Ver `CommentService.create`.
- **Un `parentCommentId` inválido (inexistente, de otro post, o no ACTIVE) devuelve 400.** Es dato del
  payload; sigue la sección 6 del spec.
- **La edición y el cambio de estado de un comentario son a nivel comentario** (`/comments/{id}`), sin
  volver a chequear la visibilidad del post: el autor/moderador puede gestionar el comentario aunque el
  post haya cambiado de estado. Si se quisiera acoplar a la visibilidad del post, revisar
  `CommentService.update`/`changeStatus`.

### Hito 6 — Votos
- **`commentCount` cuenta solo comentarios `ACTIVE`.** El spec no lo define; se optó por contar los
  comentarios activos (no los ocultos/eliminados). Si se quisiera contar todos, revisar
  `PostCommentStatsImpl` / `CommentRepository.countByPostIdsAndStatus`.
- **Votar (PUT) un post/comentario visible pero no ACTIVE devuelve 409** (caso de un moderador sobre
  contenido `HIDDEN`); para un usuario común no es visible y devuelve 404. Solo se vota contenido `ACTIVE`.
- **`DELETE .../vote` no valida rol ni existencia del contenido**: es idempotente (204 siempre) y solo
  requiere estar autenticado y no suspendido. Quitar el propio voto se considera limpieza, no una escritura
  sujeta a `*-vote-min-role`.

### Hito 7 — Imágenes
- **Una imagen WebP subida se guarda y se sirve como PNG** (se re-codifica para limpiar metadatos y no
  hay codificador WebP en Java puro). Es decir, se acepta el upload de WebP pero su `contentType` pasa a
  `image/png`. Ver `ImageProcessor`. Detalle técnico en `decisions.md`.
- **La subida/borrado de imágenes es solo del autor** (ni siquiera un moderador puede subir a un post
  ajeno), y solo si el post está `ACTIVE`. Post no visible → 404; post visible-pero-no-ACTIVE (moderador)
  → 409.
- **`position` de las imágenes arranca en 0** (máximo actual + 1, con máximo inicial −1). La miniatura es
  la imagen de menor `position`.
- **`sizeBytes` es el tamaño después de procesar** (re-codificado), no el del archivo subido, tal como
  indica el spec.

### Hito 8 — Follows
- **`followedByMe` es `null` para usuarios anónimos**; para un usuario autenticado es `true`/`false`.
  En el propio perfil (`/users/me`) queda `false` (no te seguís a vos mismo). Ver `UserService.toResponse`.
- **Seguirse a uno mismo → 400**; seguir a un usuario con rol menor a `follow-target-min-role` → 400;
  seguir a un usuario inexistente → 404. `POST` es idempotente (204 aunque ya lo siga) y `DELETE` también
  (204 aunque no lo siga).
- **Los contadores de follows en `UserResponse` cuentan todas las relaciones** (no dependen del estado del
  usuario seguido/seguidor). En el MVP no hay estado "borrado" de follows: se crean y se eliminan físicamente.

## Preguntas abiertas / a decidir en el futuro

- Nada pendiente por ahora. Las funcionalidades marcadas como "Fuera del MVP" en la
  sección 1 del spec (catálogos, verificación, cuentas de negocio, feed, notificaciones,
  denuncias, búsqueda, almacenamiento en la nube, restricciones legales/edad, JWT) se
  retomarán cuando se planifique la etapa posterior al MVP.
