# Mejoras futuras (post-launch)

Ideas de mejora que **no son bloqueantes para salir a producción** (eso vive en
`produccion-pendientes.md`). Son enhancements para hacer cuando haya tiempo/ganas, sin apuro.

---

## Imágenes: redimensionar + generar miniatura al subir

**Estado actual:** el `ImageProcessor` valida el tipo real (magic bytes) y re-codifica para borrar
metadatos/EXIF (incluida ubicación GPS), pero **no redimensiona**. Además, la "miniatura" del feed
(`PostSummaryResponse.thumbnailUrl` / `PostImageViewsImpl.thumbnailUrlByPost`) apunta a la **imagen a
tamaño completo** (`/images/{id}`): el navegador la achica por CSS pero descarga los bytes enteros.

**Síntoma observado:** una foto bajada de internet pesa ~50KB; una sacada con cámara (alta resolución)
pesa 500–700KB. No es un bug —el peso acompaña la resolución de origen y está dentro del límite de 5MB—
pero en un feed con varias fotos de cámara se baja bastante ancho de banda (se nota en mobile).

**Mejora propuesta:**
- Al subir, **redimensionar a un lado máximo** (p. ej. ~1600px para el detalle).
- **Generar una miniatura chica** (p. ej. ~400px) y servirla en el feed en vez de la imagen completa.
- Impacto estimado: foto de cámara de ~600KB → ~150–250KB (detalle) y ~30KB (miniatura); feed mucho
  más rápido.

**Dónde toca:** principalmente el `ImageProcessor` (resize) + el guardado/servido de la miniatura
(una storage key adicional o derivar la miniatura on-the-fly). El resto (storage/R2, URLs, front) no
se entera si se mantiene el modelo de servir por `/images/{id}`.
