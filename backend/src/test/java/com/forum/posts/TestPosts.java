package com.forum.posts;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.forum.categories.Category;
import com.forum.shared.ContentStatus;
import com.forum.shared.PostType;
import com.forum.users.User;

/**
 * Helper de tests para crear posts desde cualquier módulo. Vive en {@code com.forum.posts} para
 * usar el repositorio package-private y es público para que otros paquetes de test lo inyecten.
 */
@Component
public class TestPosts {

    private final PostRepository postRepository;

    TestPosts(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    public Post create(User author, PostType type, ContentStatus status, Category... cats) {
        Post post = new Post("Título de prueba", "Contenido de prueba", author, type,
                new LinkedHashSet<>(Set.of(cats)));
        if (status != ContentStatus.ACTIVE) {
            post.changeStatus(status);
        }
        return postRepository.save(post);
    }
}
