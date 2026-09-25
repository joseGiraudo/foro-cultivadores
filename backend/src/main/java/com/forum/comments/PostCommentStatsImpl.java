package com.forum.comments;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.forum.posts.PostCommentStats;
import com.forum.shared.ContentStatus;

/** Implementa la estadística que el módulo de posts necesita: cantidad de comentarios activos por post. */
@Component
class PostCommentStatsImpl implements PostCommentStats {

    private final CommentRepository commentRepository;

    PostCommentStatsImpl(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Long> commentCountsByPost(Collection<Long> postIds) {
        if (postIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> counts = new LinkedHashMap<>();
        for (Object[] row : commentRepository.countByPostIdsAndStatus(postIds, ContentStatus.ACTIVE)) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return counts;
    }
}
