package com.forum.posts;

import com.forum.users.Role;
import com.forum.users.User;

/** Autor embebido en las respuestas de posts: id, username, rol y avatar. */
record AuthorResponse(Long id, String username, Role role, String avatarUrl) {

    static AuthorResponse of(User user) {
        return new AuthorResponse(user.getId(), user.displayUsername(), user.getRole(), user.getAvatarUrl());
    }
}
