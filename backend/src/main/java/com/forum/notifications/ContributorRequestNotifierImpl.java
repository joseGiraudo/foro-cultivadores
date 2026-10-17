package com.forum.notifications;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.forum.contributorrequests.ContributorRequestNotifier;

/** Implementación de {@link ContributorRequestNotifier}: persiste la notificación de resultado. */
@Component
class ContributorRequestNotifierImpl implements ContributorRequestNotifier {

    private final NotificationRepository notificationRepository;

    ContributorRequestNotifierImpl(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    @Transactional
    public void requestResolved(Long recipientUserId, Long actorUserId, String actorUsername,
                                boolean approved) {
        NotificationType type = approved
                ? NotificationType.CONTRIBUTOR_REQUEST_APPROVED
                : NotificationType.CONTRIBUTOR_REQUEST_REJECTED;
        notificationRepository.save(new Notification(recipientUserId, actorUserId, actorUsername, type));
    }
}
