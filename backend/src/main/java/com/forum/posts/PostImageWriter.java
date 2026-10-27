package com.forum.posts;

/**
 * Escritura de imágenes de un post, usada por {@link PostService} para guardar las imágenes que
 * llegan junto a la creación del post (consultas). La implementación vive en el módulo de imágenes
 * (inversión de dependencias: así {@code posts} no depende de él, igual que {@link PostImages}).
 *
 * <p>No hace chequeos de permiso ni de visibilidad: el llamador garantiza que el post es del autor
 * y está recién creado. Sí valida el tipo real del archivo (mismo procesamiento que la subida
 * suelta: re-codifica y borra EXIF) y lanza 415 si el contenido no es una imagen permitida.
 */
public interface PostImageWriter {

    /** Procesa y persiste una imagen para el post en la siguiente posición disponible. */
    ImageResponse store(Long postId, byte[] content);
}
