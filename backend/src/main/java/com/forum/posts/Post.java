package com.forum.posts;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

import org.hibernate.annotations.BatchSize;

import com.forum.categories.Category;
import com.forum.shared.ContentStatus;
import com.forum.shared.PostType;
import com.forum.users.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "posts")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 20000)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Enumerated(EnumType.STRING)
    @Column(name = "post_type", nullable = false, length = 20)
    private PostType postType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ContentStatus status;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "post_categories",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"))
    @BatchSize(size = 50)
    private Set<Category> categories = new LinkedHashSet<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Post() {
        // requerido por JPA
    }

    public Post(String title, String content, User author, PostType postType, Set<Category> categories) {
        this.title = title;
        this.content = content;
        this.author = author;
        this.postType = postType;
        this.status = ContentStatus.ACTIVE;
        this.categories = new LinkedHashSet<>(categories);
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public User getAuthor() {
        return author;
    }

    public PostType getPostType() {
        return postType;
    }

    public ContentStatus getStatus() {
        return status;
    }

    public Set<Category> getCategories() {
        return categories;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void edit(String newTitle, String newContent, Set<Category> newCategories) {
        if (newTitle != null) {
            this.title = newTitle;
        }
        if (newContent != null) {
            this.content = newContent;
        }
        if (newCategories != null) {
            this.categories = new LinkedHashSet<>(newCategories);
        }
        this.updatedAt = Instant.now();
    }

    public void changeStatus(ContentStatus newStatus) {
        this.status = newStatus;
        this.updatedAt = Instant.now();
    }
}
