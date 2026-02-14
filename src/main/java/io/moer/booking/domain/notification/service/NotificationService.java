package io.moer.booking.domain.notification.service;

import io.moer.booking.common.exception.BusinessException;
import io.moer.booking.common.exception.EntityNotFoundException;
import io.moer.booking.common.exception.ErrorCode;
import io.moer.booking.domain.notification.Notification;
import io.moer.booking.domain.notification.NotificationType;
import io.moer.booking.domain.notification.dto.NotificationListResponse;
import io.moer.booking.domain.notification.dto.NotificationResponse;
import io.moer.booking.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;

    /**
     * 알림 목록 조회
     */
    public NotificationListResponse getNotifications(Long userId, int page, int size, boolean unreadOnly) {
        int offset = (page - 1) * size;

        List<Notification> notifications;
        int totalCount;

        if (unreadOnly) {
            notifications = notificationRepository.findUnreadByUserId(userId, offset, size);
            totalCount = notificationRepository.countUnreadByUserId(userId);
        } else {
            notifications = notificationRepository.findByUserId(userId, offset, size);
            totalCount = notificationRepository.countByUserId(userId);
        }

        int unreadCount = notificationRepository.countUnreadByUserId(userId);

        List<NotificationResponse> items = notifications.stream()
                .map(NotificationResponse::from)
                .collect(Collectors.toList());

        return NotificationListResponse.builder()
                .items(items)
                .totalCount(totalCount)
                .unreadCount(unreadCount)
                .build();
    }

    /**
     * 알림 읽음 처리
     */
    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (!notification.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOTIFICATION_ACCESS_DENIED);
        }

        notificationRepository.markAsRead(notificationId);
    }

    /**
     * 전체 알림 읽음 처리
     */
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
    }

    /**
     * 알림 생성 (내부 사용)
     */
    @Transactional
    public void createNotification(Long userId, Long businessId, NotificationType type,
                                    String title, String message, String link,
                                    String referenceType, Long referenceId) {
        Notification notification = Notification.builder()
                .userId(userId)
                .businessId(businessId)
                .type(type)
                .title(title)
                .message(message)
                .link(link)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .isRead("N")
                .build();

        notificationRepository.save(notification);
        log.debug("Notification created: userId={}, type={}, title={}", userId, type, title);
    }
}
