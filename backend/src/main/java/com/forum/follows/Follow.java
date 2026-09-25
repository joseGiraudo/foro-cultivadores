package com.forum.follows;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Relación de seguimiento en un solo sentido: {@code follower} sigue a {@code following}. */
@Entity
@Table(name = "follows")
class Follow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "follower_id", nullable = false)
    private Long followerId;

    @Column(name = "following_id", nullable = false)
    private Long followingId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Follow() {
        // requerido por JPA
    }

    Follow(Long followerId, Long followingId) {
        this.followerId = followerId;
        this.followingId = followingId;
        this.createdAt = Instant.now();
    }

    Long getFollowerId() {
        return followerId;
    }

    Long getFollowingId() {
        return followingId;
    }
}
