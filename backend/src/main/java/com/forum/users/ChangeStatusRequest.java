package com.forum.users;

import jakarta.validation.constraints.NotNull;

public record ChangeStatusRequest(
        @NotNull
        UserStatus status
) {
}
