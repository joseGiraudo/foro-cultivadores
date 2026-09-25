package com.forum.shared;

/**
 * Estado de un contenido moderable (post o comentario). Los estados {@code DELETED_*} son
 * terminales. Las transiciones válidas están en {@link ContentStatusTransitions}.
 */
public enum ContentStatus {
    ACTIVE,
    HIDDEN,
    DELETED_BY_USER,
    DELETED_BY_MOD
}
