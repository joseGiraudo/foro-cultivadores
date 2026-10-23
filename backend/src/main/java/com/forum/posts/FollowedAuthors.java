package com.forum.posts;

import java.util.List;

/**
 * Autores que un usuario sigue. Interfaz definida en {@code posts} (el consumidor) e
 * implementada en {@code follows} (inversión de dependencias, mismo patrón que
 * {@link PostVoteStats}/{@link PostCommentStats}). La usa el feed "Siguiendo".
 */
public interface FollowedAuthors {

    /** Ids de los usuarios que {@code userId} sigue (vacío si no sigue a nadie). */
    List<Long> authorsFollowedBy(Long userId);
}
