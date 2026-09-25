package com.forum.categories;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(
        @NotBlank
        @Size(max = 50)
        String name,

        @Size(max = 300)
        String description
) {
}
