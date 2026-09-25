package com.forum.categories;

public record CategoryResponse(
        Long id,
        String name,
        String description
) {

    static CategoryResponse of(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getDescription());
    }
}
