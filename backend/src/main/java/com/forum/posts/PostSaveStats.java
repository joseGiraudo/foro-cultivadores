package com.forum.posts;

import java.util.Collection;
import java.util.Set;

/**
 * Consulta de posts guardados, usada por el módulo de posts para armar sus respuestas.
 * La implementación vive en el módulo {@code saves} (inversión de dependencias: así {@code posts}
 * no depende de {@code saves}). La consulta es en lote para evitar N+1.
 */
public interface PostSaveStats {

    /** De los posts dados, cuáles guardó el usuario. Vacío si userId es null. */
    Set<Long> savedPostIds(Collection<Long> postIds, Long userId);
}
