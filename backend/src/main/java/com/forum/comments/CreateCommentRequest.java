package com.forum.comments;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
        @NotBlank
        @Size(min = 1, max = 5000)
        String content,

        Long parentCommentId
) {
}
