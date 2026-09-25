package com.forum.posts;

import com.forum.users.Role;
import com.forum.users.User;

/** Autor embebido en las respuestas de posts: id, username y rol. */
record AuthorResponse(Long id, String username, Role role) {

    static AuthorResponse of(User user) {
        return new AuthorResponse(user.getId(), user.getUsername(), user.getRole());
    }
}
