package com.forum.users;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Estadísticas de follows, consultadas por el módulo de usuarios para armar {@code UserResponse}.
 * La implementación vive en el módulo de follows (inversión de dependencias). Consultas en lote
 * para evitar N+1.
 */
public interface FollowStats {

    /** Cantidad de seguidores por usuario; los usuarios sin seguidores no aparecen (se asume 0). */
    Map<Long, Long> followerCounts(Collection<Long> userIds);

    /** Cantidad de seguidos por usuario; los usuarios que no siguen a nadie no aparecen (se asume 0). */
    Map<Long, Long> followingCounts(Collection<Long> userIds);

    /** De {@code targetIds}, cuáles sigue {@code followerId} (para calcular followedByMe en lote). */
    Set<Long> followingAmong(Long followerId, Collection<Long> targetIds);
}
