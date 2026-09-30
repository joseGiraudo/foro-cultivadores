package com.forum.saves;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Post guardado por un usuario ("bookmark"). Único por (usuario, post). */
@Entity
@Table(name = "saved_posts")
class SavedPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected SavedPost() {
        // requerido por JPA
    }

    SavedPost(Long userId, Long postId) {
        this.userId = userId;
        this.postId = postId;
        this.createdAt = Instant.now();
    }

    Long getPostId() {
        return postId;
    }
}
