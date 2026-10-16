package com.forum.notifications;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.forum.comments.CommentNotifier;

/** Implementación de {@link CommentNotifier}: persiste la notificación in-app. */
@Component
class CommentNotifierImpl implements CommentNotifier {

    private final NotificationRepository notificationRepository;

    CommentNotifierImpl(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    @Transactional
    public void commentOnPost(Long recipientUserId, Long actorUserId, String actorUsername,
                              Long postId, String postTitle, Long commentId) {
        notificationRepository.save(new Notification(recipientUserId, actorUserId, actorUsername,
                NotificationType.COMMENT_ON_POST, postId, postTitle, commentId));
    }

    @Override
    @Transactional
    public void replyToComment(Long recipientUserId, Long actorUserId, String actorUsername,
                               Long postId, String postTitle, Long commentId) {
        notificationRepository.save(new Notification(recipientUserId, actorUserId, actorUsername,
                NotificationType.REPLY_TO_COMMENT, postId, postTitle, commentId));
    }
}
