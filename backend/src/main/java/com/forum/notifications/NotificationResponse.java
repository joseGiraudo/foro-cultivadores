package com.forum.notifications;

import java.time.Instant;

/** Notificación expuesta por la API. Incluye snapshots (actor, título) para render directo. */
public record NotificationResponse(
        Long id,
        NotificationType type,
        Long actorUserId,
        String actorUsername,
        Long postId,
        String postTitle,
        Long commentId,
        boolean read,
        Instant createdAt
) {

    static NotificationResponse of(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getType(),
                n.getActorUserId(),
                n.getActorUsername(),
                n.getPostId(),
                n.getPostTitle(),
                n.getCommentId(),
                n.isRead(),
                n.getCreatedAt());
    }
}
