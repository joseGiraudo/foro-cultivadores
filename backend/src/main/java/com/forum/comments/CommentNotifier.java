package com.forum.comments;

/**
 * Notifica eventos de comentarios a quien corresponda. La implementación vive en el módulo
 * {@code notifications} (inversión de dependencias: así {@code comments} no depende de
 * {@code notifications}). Los snapshots (nombre del actor, título del post) se pasan acá para
 * que la bandeja no tenga que hacer joins al leerse.
 *
 * <p>La lógica de a quién notificar (autor del post / del comentario padre, sin auto-notificar)
 * vive en {@code CommentService}; esta interfaz solo persiste la notificación ya decidida.
 */
public interface CommentNotifier {

    /** Comentario de primer nivel en un post: se notifica al autor del post. */
    void commentOnPost(Long recipientUserId, Long actorUserId, String actorUsername,
                       Long postId, String postTitle, Long commentId);

    /** Respuesta a un comentario: se notifica al autor del comentario padre. */
    void replyToComment(Long recipientUserId, Long actorUserId, String actorUsername,
                        Long postId, String postTitle, Long commentId);
}
