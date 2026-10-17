package com.forum.notifications;

/** Tipos de notificación in-app soportados. */
public enum NotificationType {
    /** Alguien comentó (nivel superior) en un post del destinatario. */
    COMMENT_ON_POST,
    /** Alguien respondió un comentario del destinatario. */
    REPLY_TO_COMMENT,
    /** La petición del destinatario para ser CONTRIBUTOR fue aprobada. */
    CONTRIBUTOR_REQUEST_APPROVED,
    /** La petición del destinatario para ser CONTRIBUTOR fue rechazada. */
    CONTRIBUTOR_REQUEST_REJECTED
}
