package com.forum.comments;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCommentRequest(
        @NotBlank
        @Size(min = 1, max = 5000)
        String content
) {
}
