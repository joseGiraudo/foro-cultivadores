package com.forum.comments;

import java.time.Instant;

import com.forum.shared.ContentStatus;
import com.forum.shared.VoteValue;

/**
 * Ítem de la lista plana de comentarios (recorrido en profundidad). {@code content} es null cuando
 * el comentario no es visible para quien consulta (no ACTIVE y no es moderador); el {@code status}
 * siempre se muestra para no romper el hilo. {@code myVote} es null si es anónimo o no votó.
 */
public record CommentResponse(
        Long id,
        String content,
        ContentStatus status,
        CommentAuthor author,
        Long parentCommentId,
        int depth,
        int score,
        VoteValue myVote,
        boolean pinned,
        Instant createdAt,
        Instant updatedAt,
        Instant editedAt
) {
}
