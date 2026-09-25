package com.forum.posts;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Imágenes de posts, consultadas por el módulo de posts para armar sus respuestas. La implementación
 * vive en el módulo de imágenes (inversión de dependencias: así {@code posts} no depende de él).
 */
public interface PostImages {

    /** Imágenes del post ordenadas por position ascendente (para el detalle). */
    List<ImageResponse> imagesForPost(Long postId);

    /** URL de la miniatura (imagen de menor position) por post; solo incluye posts con imágenes. */
    Map<Long, String> thumbnailUrlByPost(Collection<Long> postIds);
}
