package com.forum.users;

import jakarta.validation.constraints.Size;

public record UpdateMeRequest(
        @Size(max = 500)
        String bio
) {
}
