package com.forum.comments;

import com.forum.users.User;

/** Autor embebido en las respuestas de comentarios: id y username. */
record CommentAuthor(Long id, String username) {

    static CommentAuthor of(User user) {
        return new CommentAuthor(user.getId(), user.getUsername());
    }
}
