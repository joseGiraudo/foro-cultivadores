package com.forum.images;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "post_images")
class PostImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "storage_key", nullable = false, length = 100)
    private String storageKey;

    @Column(name = "content_type", nullable = false, length = 30)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(nullable = false)
    private int position;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PostImage() {
        // requerido por JPA
    }

    PostImage(Long postId, String storageKey, String contentType, long sizeBytes, int position) {
        this.postId = postId;
        this.storageKey = storageKey;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.position = position;
        this.createdAt = Instant.now();
    }

    Long getId() {
        return id;
    }

    Long getPostId() {
        return postId;
    }

    String getStorageKey() {
        return storageKey;
    }

    String getContentType() {
        return contentType;
    }

    long getSizeBytes() {
        return sizeBytes;
    }

    int getPosition() {
        return position;
    }
}
