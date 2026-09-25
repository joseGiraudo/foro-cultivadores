package com.forum.votes;

import java.time.Instant;

import com.forum.shared.VoteValue;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "post_votes")
class PostVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private VoteValue value;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PostVote() {
        // requerido por JPA
    }

    PostVote(Long userId, Long postId, VoteValue value) {
        this.userId = userId;
        this.postId = postId;
        this.value = value;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    void changeValue(VoteValue newValue) {
        this.value = newValue;
        this.updatedAt = Instant.now();
    }

    VoteValue getValue() {
        return value;
    }
}
