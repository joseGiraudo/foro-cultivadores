package com.forum.users;

import jakarta.validation.constraints.NotNull;

public record ChangeRoleRequest(
        @NotNull
        Role role
) {
}
