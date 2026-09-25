package com.forum.posts;

import java.util.Collection;
import java.util.Map;

import com.forum.shared.VoteValue;

/**
 * Estadísticas de votos de posts, consultadas por el módulo de posts para armar sus respuestas.
 * La implementación vive en el módulo de votos (inversión de dependencias: así {@code posts} no
 * depende de {@code votes}). Las consultas son en lote para evitar N+1.
 */
public interface PostVoteStats {

    /** score (upvotes − downvotes) por post; los posts sin votos no aparecen (se asume 0). */
    Map<Long, Integer> scoresByPost(Collection<Long> postIds);

    /** Voto del usuario dado por post; solo incluye los posts donde votó. Vacío si userId es null. */
    Map<Long, VoteValue> myVotesByPost(Collection<Long> postIds, Long userId);
}
