package com.forum.rules;

/** Criterio de ordenamiento de los comentarios de primer nivel de un post. */
public enum CommentOrder {
    /** Score descendente y, en empate, createdAt ascendente. */
    SCORE,
    /** createdAt ascendente. */
    CHRONOLOGICAL
}
