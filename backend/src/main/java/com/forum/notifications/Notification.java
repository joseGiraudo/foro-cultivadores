package com.forum.notifications;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Notificación in-app para un usuario. Guarda snapshots de {@code actorUsername} y
 * {@code postTitle} para renderizar la bandeja sin joins ni dependencias de otros módulos.
 */
@Entity
@Table(name = "notifications")
class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipient_user_id", nullable = false)
    private Long recipientUserId;

    @Column(name = "actor_user_id", nullable = false)
    private Long actorUserId;

    @Column(name = "actor_username", nullable = false, length = 30)
    private String actorUsername;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "post_title", nullable = false, length = 150)
    private String postTitle;

    @Column(name = "comment_id")
    private Long commentId;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Notification() {
        // requerido por JPA
    }

    Notification(Long recipientUserId, Long actorUserId, String actorUsername, NotificationType type,
                 Long postId, String postTitle, Long commentId) {
        this.recipientUserId = recipientUserId;
        this.actorUserId = actorUserId;
        this.actorUsername = actorUsername;
        this.type = type;
        this.postId = postId;
        this.postTitle = postTitle;
        this.commentId = commentId;
        this.read = false;
        this.createdAt = Instant.now();
    }

    Long getId() {
        return id;
    }

    Long getRecipientUserId() {
        return recipientUserId;
    }

    Long getActorUserId() {
        return actorUserId;
    }

    String getActorUsername() {
        return actorUsername;
    }

    NotificationType getType() {
        return type;
    }

    Long getPostId() {
        return postId;
    }

    String getPostTitle() {
        return postTitle;
    }

    Long getCommentId() {
        return commentId;
    }

    boolean isRead() {
        return read;
    }

    void markRead() {
        this.read = true;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
