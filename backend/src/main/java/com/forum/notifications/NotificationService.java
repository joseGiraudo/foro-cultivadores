package com.forum.notifications;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.forum.shared.error.ApiException;
import com.forum.users.AuthUser;

@Service
class NotificationService {

    private final NotificationRepository notificationRepository;

    NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> list(AuthUser principal, Pageable pageable) {
        return notificationRepository.findByRecipientUserId(principal.getId(), pageable)
                .map(NotificationResponse::of);
    }

    @Transactional(readOnly = true)
    public long unreadCount(AuthUser principal) {
        return notificationRepository.countByRecipientUserIdAndReadFalse(principal.getId());
    }

    /** Marca una notificación como leída. 404 si no existe o no es del usuario. */
    @Transactional
    public void markRead(Long id, AuthUser principal) {
        Notification notification = notificationRepository.findById(id)
                .filter(n -> n.getRecipientUserId().equals(principal.getId()))
                .orElseThrow(() -> ApiException.notFound("Notificación no encontrada"));
        notification.markRead();
    }

    @Transactional
    public void markAllRead(AuthUser principal) {
        notificationRepository.markAllReadForUser(principal.getId());
    }
}
