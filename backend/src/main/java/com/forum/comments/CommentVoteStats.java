package com.forum.comments;

import java.util.Collection;
import java.util.Map;

import com.forum.shared.VoteValue;

/**
 * Estadísticas de votos de comentarios, consultadas por el módulo de comentarios para armar sus
 * respuestas y ordenar por score. La implementación vive en el módulo de votos (inversión de
 * dependencias). Consultas en lote para evitar N+1.
 */
public interface CommentVoteStats {

    /** score por comentario; los comentarios sin votos no aparecen (se asume 0). */
    Map<Long, Integer> scoresByComment(Collection<Long> commentIds);

    /** Voto del usuario dado por comentario; solo los que votó. Vacío si userId es null. */
    Map<Long, VoteValue> myVotesByComment(Collection<Long> commentIds, Long userId);
}
