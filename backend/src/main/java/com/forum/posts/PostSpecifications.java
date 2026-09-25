package com.forum.posts;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.forum.shared.ContentStatus;
import com.forum.shared.PostType;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;

/** Construye el filtro dinámico de listado de posts. */
final class PostSpecifications {

    private PostSpecifications() {
    }

    static Specification<Post> filter(Long categoryId, Long authorId, PostType postType, ContentStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (authorId != null) {
                predicates.add(cb.equal(root.get("author").get("id"), authorId));
            }
            if (postType != null) {
                predicates.add(cb.equal(root.get("postType"), postType));
            }
            if (categoryId != null) {
                if (query != null) {
                    query.distinct(true);
                }
                Join<Object, Object> categories = root.join("categories");
                predicates.add(cb.equal(categories.get("id"), categoryId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
