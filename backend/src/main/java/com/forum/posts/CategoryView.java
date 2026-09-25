package com.forum.posts;

import com.forum.categories.Category;

/** Categoría embebida en las respuestas de posts: id y nombre. */
record CategoryView(Long id, String name) {

    static CategoryView of(Category category) {
        return new CategoryView(category.getId(), category.getName());
    }
}
