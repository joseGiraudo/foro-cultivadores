package com.forum.posts;

import com.forum.shared.ContentStatus;

import jakarta.validation.constraints.NotNull;

public record ChangePostStatusRequest(
        @NotNull
        ContentStatus status
) {
}
