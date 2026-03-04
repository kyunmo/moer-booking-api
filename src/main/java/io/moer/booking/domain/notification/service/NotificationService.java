package io.moer.booking.domain.notification.service;

import io.moer.booking.common.exception.BusinessException;
import io.moer.booking.common.exception.EntityNotFoundException;
import io.moer.booking.common.exception.ErrorCode;
import io.moer.booking.domain.customer.Customer;
import io.moer.booking.domain.customer.repository.CustomerRepository;
import io.moer.booking.domain.notification.Notification;
import io.moer.booking.domain.notification.NotificationType;
import io.moer.booking.domain.notification.dto.NotificationListResponse;
import io.moer.booking.domain.notification.dto.NotificationResponse;
import io.moer.booking.domain.notification.dto.NotificationSendRequest;
import io.moer.booking.domain.notification.dto.NotificationSendResponse;
import io.moer.booking.domain.notification.repository.NotificationRepository;
import io.moer.booking.domain.notificationlog.NotificationChannel;
import io.moer.booking.domain.notificationlog.NotificationLog;
import io.moer.booking.domain.notificationlog.NotificationLogStatus;
import io.moer.booking.domain.notificationlog.NotificationTemplateType;
import io.moer.booking.domain.notificationlog.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private static final Set<String> VALID_CHANNELS = Set.of("APP_PUSH", "KAKAO_ALIMTALK", "SMS");
    private static final Set<String> VALID_TARGET_TYPES = Set.of("ALL", "SPECIFIC", "SEGMENT");
    private static final Set<String> VALID_SEGMENTS = Set.of("VIP", "REGULAR", "NEW", "INACTIVE", "BIRTHDAY");

    private final NotificationRepository notificationRepository;
    private final CustomerRepository customerRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final SseEmitterService sseEmitterService;

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

    /**
     * 관리자 고객 알림 발송
     */
    @Transactional
    public NotificationSendResponse sendToCustomers(Long businessId, NotificationSendRequest request, Long senderId) {
        // 1. 채널 유효성 검증
        validateChannels(request.getChannels());

        // 2. targetType에 따라 대상 고객 조회
        List<Customer> targetCustomers = resolveTargetCustomers(businessId, request);

        // 3. 대상이 0명이면 예외
        if (targetCustomers.isEmpty()) {
            throw new BusinessException(ErrorCode.NOTIFICATION_TARGET_EMPTY,
                    "발송 대상 고객이 없습니다");
        }

        log.info("Sending notification to {} customers: businessId={}, senderId={}, targetType={}, title={}",
                targetCustomers.size(), businessId, senderId, request.getTargetType(), request.getTitle());

        int successCount = 0;
        int failCount = 0;
        Long firstLogId = null;
        LocalDateTime sentAt = LocalDateTime.now();

        // 4. 각 고객에 대해 알림 생성 및 발송 로그 기록
        for (Customer customer : targetCustomers) {
            try {
                // 고객의 userId가 있으면 앱 내 알림 + SSE 전달
                if (customer.getUserId() != null) {
                    Notification notification = Notification.builder()
                            .userId(customer.getUserId())
                            .businessId(businessId)
                            .type(NotificationType.SYSTEM)
                            .title(request.getTitle())
                            .message(request.getMessage())
                            .referenceType("NOTIFICATION_SEND")
                            .isRead("N")
                            .build();
                    notificationRepository.save(notification);

                    // SSE 실시간 전달
                    if (sseEmitterService.isConnected(customer.getUserId())) {
                        sseEmitterService.sendEvent(customer.getUserId(), "NOTIFICATION",
                                NotificationResponse.from(notification));
                    }
                }

                // 외부 채널별 발송 로그 기록
                for (String channel : request.getChannels()) {
                    NotificationLog notificationLog = NotificationLog.builder()
                            .businessId(businessId)
                            .channel(mapChannel(channel))
                            .templateType(NotificationTemplateType.RESERVATION_REMINDER)
                            .recipientPhone(customer.getPhone())
                            .recipientName(customer.getName())
                            .title(request.getTitle())
                            .content(request.getMessage())
                            .status(NotificationLogStatus.SENT)
                            .sentAt(sentAt)
                            .build();
                    notificationLogRepository.save(notificationLog);

                    if (firstLogId == null) {
                        firstLogId = notificationLog.getId();
                    }

                    log.debug("[NotificationSend] channel={}, recipient={}, phone={}",
                            channel, customer.getName(), customer.getPhone());
                }

                successCount++;
            } catch (Exception e) {
                failCount++;
                log.warn("Failed to send notification to customer: customerId={}, error={}",
                        customer.getId(), e.getMessage());
            }
        }

        log.info("Notification send completed: businessId={}, total={}, success={}, fail={}",
                businessId, targetCustomers.size(), successCount, failCount);

        return NotificationSendResponse.builder()
                .notificationLogId(firstLogId)
                .targetCount(targetCustomers.size())
                .scheduledAt(request.getScheduledAt())
                .sentAt(sentAt)
                .status(failCount == 0 ? "SENT" : (successCount == 0 ? "FAILED" : "PARTIAL"))
                .channels(request.getChannels())
                .successCount(successCount)
                .failCount(failCount)
                .build();
    }

    /**
     * 채널 유효성 검증
     */
    private void validateChannels(List<String> channels) {
        for (String channel : channels) {
            if (!VALID_CHANNELS.contains(channel)) {
                throw new BusinessException(ErrorCode.NOTIFICATION_INVALID_CHANNEL,
                        "유효하지 않은 발송 채널입니다: " + channel);
            }
        }
    }

    /**
     * targetType에 따라 대상 고객 목록 조회
     */
    private List<Customer> resolveTargetCustomers(Long businessId, NotificationSendRequest request) {
        String targetType = request.getTargetType();

        if (!VALID_TARGET_TYPES.contains(targetType)) {
            throw new BusinessException(ErrorCode.NOTIFICATION_INVALID_CHANNEL,
                    "유효하지 않은 발송 대상 유형입니다: " + targetType);
        }

        return switch (targetType) {
            case "ALL" -> customerRepository.findByBusinessId(businessId);
            case "SPECIFIC" -> resolveSpecificCustomers(businessId, request.getTargetIds());
            case "SEGMENT" -> resolveSegmentCustomers(businessId, request.getSegment());
            default -> List.of();
        };
    }

    /**
     * SPECIFIC: 개별 고객 ID로 조회
     */
    private List<Customer> resolveSpecificCustomers(Long businessId, List<Long> targetIds) {
        if (targetIds == null || targetIds.isEmpty()) {
            throw new BusinessException(ErrorCode.NOTIFICATION_TARGET_EMPTY,
                    "발송 대상 고객 ID가 비어있습니다");
        }

        List<Customer> customers = new ArrayList<>();
        for (Long customerId : targetIds) {
            customerRepository.findById(customerId)
                    .filter(c -> c.getBusinessId().equals(businessId))
                    .ifPresent(customers::add);
        }
        return customers;
    }

    /**
     * SEGMENT: 세그먼트 타입별 고객 조회
     */
    private List<Customer> resolveSegmentCustomers(Long businessId, String segment) {
        if (segment == null || !VALID_SEGMENTS.contains(segment)) {
            throw new BusinessException(ErrorCode.INVALID_SEGMENT_TYPE,
                    "유효하지 않은 세그먼트입니다: " + segment);
        }

        return switch (segment) {
            case "VIP" -> customerRepository.findVipCustomers(businessId);
            case "REGULAR" -> customerRepository.findRegularCustomers(businessId);
            case "NEW" -> customerRepository.findNewCustomers(businessId);
            case "INACTIVE" -> customerRepository.findInactiveCustomers(businessId, 3);
            case "BIRTHDAY" -> customerRepository.findBirthdayCustomers(businessId, 7);
            default -> List.of();
        };
    }

    /**
     * 채널 문자열을 NotificationChannel enum으로 변환
     */
    private NotificationChannel mapChannel(String channel) {
        return switch (channel) {
            case "KAKAO_ALIMTALK" -> NotificationChannel.KAKAO;
            case "SMS" -> NotificationChannel.SMS;
            case "APP_PUSH" -> NotificationChannel.EMAIL; // APP_PUSH는 별도 채널 없으므로 EMAIL로 대체 기록
            default -> NotificationChannel.SMS;
        };
    }
}
