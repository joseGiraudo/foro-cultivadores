package com.forum.posts;

import java.time.Instant;
import java.util.List;

import com.forum.shared.ContentStatus;
import com.forum.shared.PostType;
import com.forum.shared.VoteValue;

/**
 * Resumen de un post para listados: sin contenido completo (solo un extracto) y sin imágenes,
 * pero con la url de la miniatura (imagen de menor position, null si no tiene).
 */
public record PostSummaryResponse(
        Long id,
        String title,
        String excerpt,
        PostType postType,
        ContentStatus status,
        AuthorResponse author,
        List<CategoryView> categories,
        String thumbnailUrl,
        int score,
        long commentCount,
        VoteValue myVote,
        boolean saved,
        long saveCount,
        Instant createdAt,
        Instant updatedAt,
        Instant editedAt
) {

    static PostSummaryResponse of(Post post, int score, long commentCount, VoteValue myVote, String thumbnailUrl,
                                  boolean saved, long saveCount) {
        return new PostSummaryResponse(
                post.getId(),
                post.getTitle(),
                PostMappings.excerpt(post.getContent()),
                post.getPostType(),
                post.getStatus(),
                AuthorResponse.of(post.getAuthor()),
                PostMappings.categoryViews(post),
                thumbnailUrl,
                score,
                commentCount,
                myVote,
                saved,
                saveCount,
                post.getCreatedAt(),
                post.getUpdatedAt(),
                post.getEditedAt());
    }
}
