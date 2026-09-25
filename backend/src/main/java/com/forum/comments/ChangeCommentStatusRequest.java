package com.forum.comments;

import com.forum.shared.ContentStatus;

import jakarta.validation.constraints.NotNull;

public record ChangeCommentStatusRequest(
        @NotNull
        ContentStatus status
) {
}
