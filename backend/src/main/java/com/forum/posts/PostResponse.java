package com.forum.posts;

import java.time.Instant;
import java.util.List;

import com.forum.shared.ContentStatus;
import com.forum.shared.PostType;
import com.forum.shared.VoteValue;

/**
 * Detalle de un post. {@code myVote} es null si quien consulta es anónimo o no votó.
 * {@code images} va ordenado por position.
 */
public record PostResponse(
        Long id,
        String title,
        String content,
        PostType postType,
        ContentStatus status,
        AuthorResponse author,
        List<CategoryView> categories,
        List<ImageResponse> images,
        int score,
        int upvotes,
        int downvotes,
        long commentCount,
        VoteValue myVote,
        boolean saved,
        long saveCount,
        Instant createdAt,
        Instant updatedAt,
        Instant editedAt
) {

    static PostResponse of(Post post, VoteCount votes, long commentCount, VoteValue myVote, List<ImageResponse> images,
                           boolean saved, long saveCount) {
        return new PostResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getPostType(),
                post.getStatus(),
                AuthorResponse.of(post.getAuthor()),
                PostMappings.categoryViews(post),
                images,
                votes.score(),
                votes.upvotes(),
                votes.downvotes(),
                commentCount,
                myVote,
                saved,
                saveCount,
                post.getCreatedAt(),
                post.getUpdatedAt(),
                post.getEditedAt());
    }
}
