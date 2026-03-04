package io.moer.booking.domain.broadcast.service;

import io.moer.booking.common.exception.EntityNotFoundException;
import io.moer.booking.common.exception.ErrorCode;
import io.moer.booking.domain.broadcast.Broadcast;
import io.moer.booking.domain.broadcast.dto.BroadcastCreateRequest;
import io.moer.booking.domain.broadcast.dto.BroadcastResponse;
import io.moer.booking.domain.broadcast.repository.BroadcastRepository;
import io.moer.booking.domain.business.Business;
import io.moer.booking.domain.business.BusinessStatus;
import io.moer.booking.domain.business.dto.BusinessSearchCondition;
import io.moer.booking.domain.business.repository.BusinessRepository;
import io.moer.booking.domain.notification.NotificationType;
import io.moer.booking.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BroadcastService {

    private final BroadcastRepository broadcastRepository;
    private final BusinessRepository businessRepository;
    private final NotificationService notificationService;

    @Transactional
    public BroadcastResponse createAndSendBroadcast(Long sentByUserId, BroadcastCreateRequest request) {
        String targetType = (request.getTargetType() == null || request.getTargetType().isBlank())
                ? "ALL" : request.getTargetType().toUpperCase();
        String priority = (request.getPriority() == null || request.getPriority().isBlank())
                ? "NORMAL" : request.getPriority().toUpperCase();

        // 대상 매장 소유자 조회
        List<Business> targetBusinesses = getTargetBusinesses(targetType);
        int recipientCount = targetBusinesses.size();

        Broadcast broadcast = Broadcast.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .targetType(targetType)
                .priority(priority)
                .sentBy(sentByUserId)
                .sentAt(LocalDateTime.now())
                .status("SENT")
                .recipientCount(recipientCount)
                .build();

        broadcastRepository.save(broadcast);

        // 각 매장 소유자에게 알림 생성
        for (Business business : targetBusinesses) {
            notificationService.createNotification(
                    business.getOwnerId(),
                    business.getId(),
                    NotificationType.SYSTEM,
                    "[공지] " + request.getTitle(),
                    request.getContent(),
                    null,
                    "BROADCAST",
                    broadcast.getId()
            );
        }

        log.info("Broadcast sent: id={}, targetType={}, recipientCount={}", broadcast.getId(), targetType, recipientCount);
        return BroadcastResponse.from(broadcast);
    }

    public BroadcastResponse getBroadcast(Long id) {
        Broadcast broadcast = broadcastRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.BROADCAST_NOT_FOUND));
        return BroadcastResponse.from(broadcast);
    }

    public List<BroadcastResponse> getBroadcasts(int page, int size) {
        int offset = (page - 1) * size;
        return broadcastRepository.findAll(offset, size).stream()
                .map(BroadcastResponse::from)
                .collect(Collectors.toList());
    }

    public List<BroadcastResponse> getSentBroadcasts(int page, int size) {
        int offset = (page - 1) * size;
        return broadcastRepository.findSentBroadcasts(offset, size).stream()
                .map(BroadcastResponse::from)
                .collect(Collectors.toList());
    }

    private List<Business> getTargetBusinesses(String targetType) {
        // 전체 활성 매장 조회
        BusinessSearchCondition condition = new BusinessSearchCondition();
        condition.setStatus(BusinessStatus.ACTIVE);
        condition.setPage(1);
        condition.setSize(10000); // 전체 조회

        List<Business> allBusinesses = businessRepository.findAll(condition);

        return switch (targetType) {
            case "PAID" -> allBusinesses.stream()
                    .filter(b -> b.getSubscriptionStatus() != null && "ACTIVE".equals(b.getSubscriptionStatus().name()))
                    .collect(Collectors.toList());
            case "TRIAL" -> allBusinesses.stream()
                    .filter(b -> b.getSubscriptionStatus() != null && "TRIAL".equals(b.getSubscriptionStatus().name()))
                    .collect(Collectors.toList());
            case "FREE" -> allBusinesses.stream()
                    .filter(b -> b.getSubscriptionPlan() != null && "FREE".equals(b.getSubscriptionPlan().name()))
                    .collect(Collectors.toList());
            default -> allBusinesses; // ALL
        };
    }
}
