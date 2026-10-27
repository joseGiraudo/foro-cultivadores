package com.forum.posts;

import java.util.List;

import com.forum.shared.PostType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePostRequest(
        @NotBlank
        @Size(min = 5, max = 150)
        String title,

        @NotBlank
        @Size(min = 1, max = 20000)
        String content,

        @NotNull
        PostType postType,

        @NotNull
        @Size(min = 1, max = 4, message = "un post debe tener entre 1 y 4 categorías")
        List<Long> categoryIds
) {
}
