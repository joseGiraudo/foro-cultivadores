package com.forum.comments;

import com.forum.users.User;

/** Autor embebido en las respuestas de comentarios: id, username y avatar. */
record CommentAuthor(Long id, String username, String avatarUrl) {

    static CommentAuthor of(User user) {
        return new CommentAuthor(user.getId(), user.displayUsername(), user.getAvatarUrl());
    }
}
