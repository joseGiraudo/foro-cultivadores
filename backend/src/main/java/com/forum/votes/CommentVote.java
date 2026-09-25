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
@Table(name = "comment_votes")
class CommentVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "comment_id", nullable = false)
    private Long commentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private VoteValue value;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CommentVote() {
        // requerido por JPA
    }

    CommentVote(Long userId, Long commentId, VoteValue value) {
        this.userId = userId;
        this.commentId = commentId;
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
