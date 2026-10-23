package com.forum.posts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.forum.shared.ContentStatus;
import com.forum.shared.PostType;

import jakarta.persistence.criteria.Predicate;

/**
 * Filtro dinámico del listado "simple" de posts (sin búsqueda de texto ni filtro por categorías;
 * esos casos usan queries nativas dedicadas con su propio orden: ver {@link PostRepository}).
 */
final class PostSpecifications {

    private PostSpecifications() {
    }

    /**
     * @param authorIds si no es null, restringe a posts de esos autores (feed "siguiendo").
     *                  Se asume no vacío (el service corta el caso vacío → página vacía).
     */
    static Specification<Post> filter(Long authorId, PostType postType, ContentStatus status,
                                      Collection<Long> authorIds) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (authorId != null) {
                predicates.add(cb.equal(root.get("author").get("id"), authorId));
            }
            if (authorIds != null) {
                predicates.add(root.get("author").get("id").in(authorIds));
            }
            if (postType != null) {
                predicates.add(cb.equal(root.get("postType"), postType));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
