package io.moer.booking.domain.notificationlog.service;

import io.moer.booking.domain.notificationlog.NotificationChannel;
import io.moer.booking.domain.notificationlog.NotificationLog;
import io.moer.booking.domain.notificationlog.NotificationLogStatus;
import io.moer.booking.domain.notificationlog.NotificationTemplateType;
import io.moer.booking.domain.notificationlog.dto.NotificationSender;
import io.moer.booking.domain.notificationlog.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 로그 기반 알림 발송 구현체 (카카오 연동 전 임시)
 * <p>
 * 실제 외부 알림 발송 대신 DB에 발송 기록만 남긴다.
 * 추후 KakaoNotificationSender 등으로 교체 예정.
 */
@Slf4j
@Primary
@Service
@RequiredArgsConstructor
public class LogNotificationSender implements NotificationSender {

    private final NotificationLogRepository notificationLogRepository;

    @Override
    @Transactional
    public void sendReservationCreated(Long businessId, Long reservationId,
                                        String recipientPhone, String recipientName,
                                        Map<String, String> params) {
        String title = "예약이 생성되었습니다";
        String content = buildContent(NotificationTemplateType.RESERVATION_CREATED, recipientName, params);

        saveLog(businessId, reservationId, NotificationChannel.KAKAO,
                NotificationTemplateType.RESERVATION_CREATED,
                recipientPhone, recipientName, title, content);
    }

    @Override
    @Transactional
    public void sendReservationConfirmed(Long businessId, Long reservationId,
                                          String recipientPhone, String recipientName,
                                          Map<String, String> params) {
        String title = "예약이 확정되었습니다";
        String content = buildContent(NotificationTemplateType.RESERVATION_CONFIRMED, recipientName, params);

        saveLog(businessId, reservationId, NotificationChannel.KAKAO,
                NotificationTemplateType.RESERVATION_CONFIRMED,
                recipientPhone, recipientName, title, content);
    }

    @Override
    @Transactional
    public void sendReservationCancelled(Long businessId, Long reservationId,
                                          String recipientPhone, String recipientName,
                                          Map<String, String> params) {
        String title = "예약이 취소되었습니다";
        String content = buildContent(NotificationTemplateType.RESERVATION_CANCELLED, recipientName, params);

        saveLog(businessId, reservationId, NotificationChannel.KAKAO,
                NotificationTemplateType.RESERVATION_CANCELLED,
                recipientPhone, recipientName, title, content);
    }

    @Override
    @Transactional
    public void sendReviewRequest(Long businessId, Long reservationId,
                                   String recipientPhone, String recipientName,
                                   Map<String, String> params) {
        String title = "서비스는 만족스러우셨나요?";
        String content = buildContent(NotificationTemplateType.REVIEW_REQUEST, recipientName, params);

        saveLog(businessId, reservationId, NotificationChannel.KAKAO,
                NotificationTemplateType.REVIEW_REQUEST,
                recipientPhone, recipientName, title, content);
    }

    /**
     * 알림 발송 로그 저장
     */
    private void saveLog(Long businessId, Long reservationId,
                          NotificationChannel channel, NotificationTemplateType templateType,
                          String recipientPhone, String recipientName,
                          String title, String content) {
        NotificationLog notificationLog = NotificationLog.builder()
                .businessId(businessId)
                .reservationId(reservationId)
                .channel(channel)
                .templateType(templateType)
                .recipientPhone(recipientPhone)
                .recipientName(recipientName)
                .title(title)
                .content(content)
                .status(NotificationLogStatus.SENT)
                .sentAt(LocalDateTime.now())
                .build();

        notificationLogRepository.save(notificationLog);

        log.info("[NotificationLog] {} 알림 기록 저장: businessId={}, reservationId={}, recipient={}, channel={}",
                templateType.getDescription(), businessId, reservationId, recipientName, channel);
    }

    /**
     * 템플릿 기반 내용 생성
     */
    private String buildContent(NotificationTemplateType templateType, String recipientName,
                                 Map<String, String> params) {
        String businessName = params != null ? params.getOrDefault("businessName", "") : "";
        String date = params != null ? params.getOrDefault("date", "") : "";
        String time = params != null ? params.getOrDefault("time", "") : "";
        String serviceName = params != null ? params.getOrDefault("serviceName", "") : "";

        return switch (templateType) {
            case RESERVATION_CREATED -> String.format(
                    "%s님, %s에 예약이 생성되었습니다.\n일시: %s %s\n서비스: %s",
                    recipientName, businessName, date, time, serviceName);
            case RESERVATION_CONFIRMED -> String.format(
                    "%s님, %s 예약이 확정되었습니다.\n일시: %s %s\n서비스: %s",
                    recipientName, businessName, date, time, serviceName);
            case RESERVATION_REMINDER -> String.format(
                    "%s님, 내일 %s 예약이 있습니다.\n일시: %s %s\n서비스: %s",
                    recipientName, businessName, date, time, serviceName);
            case RESERVATION_CHANGED -> String.format(
                    "%s님, %s 예약이 변경되었습니다.\n변경된 일시: %s %s",
                    recipientName, businessName, date, time);
            case RESERVATION_CANCELLED -> String.format(
                    "%s님, %s 예약이 취소되었습니다.\n일시: %s %s",
                    recipientName, businessName, date, time);
            case REVIEW_REQUEST -> String.format(
                    "%s님, %s에서의 서비스는 만족스러우셨나요?\n리뷰를 남겨주시면 감사하겠습니다.",
                    recipientName, businessName);
        };
    }
}
