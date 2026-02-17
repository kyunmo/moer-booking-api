package io.moer.booking.domain.notificationlog.service;

import io.moer.booking.common.dto.PageResponse;
import io.moer.booking.common.exception.EntityNotFoundException;
import io.moer.booking.common.exception.ErrorCode;
import io.moer.booking.domain.notificationlog.NotificationChannel;
import io.moer.booking.domain.notificationlog.NotificationLog;
import io.moer.booking.domain.notificationlog.NotificationLogStatus;
import io.moer.booking.domain.notificationlog.NotificationTemplateType;
import io.moer.booking.domain.notificationlog.dto.NotificationLogResponse;
import io.moer.booking.domain.notificationlog.dto.NotificationLogSearchCondition;
import io.moer.booking.domain.notificationlog.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 알림 발송 이력 조회 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationLogService {

    private final NotificationLogRepository notificationLogRepository;

    /**
     * 알림 발송 이력 단건 조회
     */
    public NotificationLogResponse getNotificationLog(Long id) {
        NotificationLog notificationLog = notificationLogRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        ErrorCode.NOTIFICATION_LOG_NOT_FOUND,
                        "알림 발송 이력을 찾을 수 없습니다: " + id));

        return NotificationLogResponse.from(notificationLog);
    }

    /**
     * 알림 발송 이력 페이징 조회
     */
    public PageResponse<NotificationLogResponse> getNotificationLogs(
            Long businessId, NotificationChannel channel,
            NotificationLogStatus status, NotificationTemplateType templateType,
            int page, int size) {

        int offset = (page - 1) * size;

        NotificationLogSearchCondition condition = NotificationLogSearchCondition.builder()
                .businessId(businessId)
                .channel(channel)
                .status(status)
                .templateType(templateType)
                .offset(offset)
                .size(size)
                .build();

        List<NotificationLog> logs = notificationLogRepository.findByCondition(condition);
        int totalCount = notificationLogRepository.countByCondition(condition);

        List<NotificationLogResponse> content = logs.stream()
                .map(NotificationLogResponse::from)
                .collect(Collectors.toList());

        return PageResponse.of(content, page, size, totalCount);
    }

    /**
     * 알림 발송 상태 업데이트
     */
    @Transactional
    public void updateStatus(Long id, NotificationLogStatus status, String errorMessage) {
        notificationLogRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        ErrorCode.NOTIFICATION_LOG_NOT_FOUND,
                        "알림 발송 이력을 찾을 수 없습니다: " + id));

        notificationLogRepository.updateStatus(id, status, errorMessage);

        log.info("NotificationLog status updated: id={}, status={}", id, status);
    }
}
