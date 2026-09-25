package com.forum.posts;

import java.util.Collection;
import java.util.Map;

/**
 * Cantidad de comentarios por post, consultada por el módulo de posts. La implementación vive en
 * el módulo de comentarios (inversión de dependencias). Consulta en lote para evitar N+1.
 */
public interface PostCommentStats {

    /** Cantidad de comentarios activos por post; los posts sin comentarios no aparecen (se asume 0). */
    Map<Long, Long> commentCountsByPost(Collection<Long> postIds);
}
