package com.forum.posts;

import java.util.List;

/** Utilidades de mapeo compartidas entre PostResponse y PostSummaryResponse. */
final class PostMappings {

    static final int EXCERPT_MAX_LENGTH = 200;

    private PostMappings() {
    }

    static List<CategoryView> categoryViews(Post post) {
        return post.getCategories().stream()
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .map(CategoryView::of)
                .toList();
    }

    /** Extracto del contenido para los listados: hasta EXCERPT_MAX_LENGTH caracteres. */
    static String excerpt(String content) {
        if (content == null) {
            return null;
        }
        if (content.length() <= EXCERPT_MAX_LENGTH) {
            return content;
        }
        return content.substring(0, EXCERPT_MAX_LENGTH);
    }
}
