package com.forum.users;

import java.time.Instant;

/**
 * Perfil de usuario expuesto por la API. El {@code email} solo se completa en {@code /users/me}
 * y en los endpoints de administración; en el perfil público va {@code null}.
 * {@code followedByMe} es null si quien consulta es anónimo.
 */
public record UserResponse(
        Long id,
        String username,
        String email,
        String bio,
        Role role,
        UserStatus status,
        long followerCount,
        long followingCount,
        Boolean followedByMe,
        Instant createdAt
) {

    static UserResponse of(User user, boolean includeEmail, long followerCount, long followingCount,
                           Boolean followedByMe) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                includeEmail ? user.getEmail() : null,
                user.getBio(),
                user.getRole(),
                user.getStatus(),
                followerCount,
                followingCount,
                followedByMe,
                user.getCreatedAt());
    }
}
