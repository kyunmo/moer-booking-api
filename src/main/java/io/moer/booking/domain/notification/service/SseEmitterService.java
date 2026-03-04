package io.moer.booking.domain.notification.service;

import io.moer.booking.domain.business.Business;
import io.moer.booking.domain.business.repository.BusinessRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class SseEmitterService {

    private static final Long TIMEOUT = 60L * 1000 * 30; // 30분
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    private final BusinessRepository businessRepository;

    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT);

        emitters.put(userId, emitter);

        emitter.onCompletion(() -> {
            emitters.remove(userId);
            log.debug("SSE connection completed: userId={}", userId);
        });

        emitter.onTimeout(() -> {
            emitters.remove(userId);
            log.debug("SSE connection timed out: userId={}", userId);
        });

        emitter.onError(e -> {
            emitters.remove(userId);
            log.debug("SSE connection error: userId={}", userId);
        });

        // 초기 연결 이벤트 전송
        try {
            emitter.send(SseEmitter.event()
                    .name("CONNECTED")
                    .data(Map.of("message", "SSE 연결 성공")));
        } catch (IOException e) {
            emitters.remove(userId);
            log.error("Failed to send initial SSE event: userId={}", userId, e);
        }

        log.info("SSE subscriber added: userId={}", userId);
        return emitter;
    }

    public void sendEvent(Long userId, String eventName, Object data) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data));
                log.debug("SSE event sent: userId={}, event={}", userId, eventName);
            } catch (IOException e) {
                emitters.remove(userId);
                log.warn("Failed to send SSE event, removing emitter: userId={}", userId);
            }
        }
    }

    public void sendEventToBusinessOwner(Long businessId, String eventName, Object data) {
        try {
            Business business = businessRepository.findById(businessId).orElse(null);
            if (business == null || business.getOwnerId() == null) {
                log.debug("Cannot send SSE to business owner: businessId={}, business or ownerId is null", businessId);
                return;
            }

            Long ownerId = business.getOwnerId();
            sendEvent(ownerId, eventName, data);
            log.debug("SSE event sent to business owner: businessId={}, ownerId={}, event={}", businessId, ownerId, eventName);
        } catch (Exception e) {
            log.warn("Failed to send SSE event to business owner: businessId={}, error={}", businessId, e.getMessage());
        }
    }

    public boolean isConnected(Long userId) {
        return emitters.containsKey(userId);
    }

    public int getActiveConnectionCount() {
        return emitters.size();
    }

    @Scheduled(fixedRate = 30000)
    public void sendHeartbeat() {
        Map<String, Object> heartbeat = Map.of(
                "type", "HEARTBEAT",
                "timestamp", LocalDateTime.now().toString()
        );
        emitters.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event().name("HEARTBEAT").data(heartbeat));
            } catch (Exception e) {
                emitters.remove(userId);
                log.debug("Heartbeat failed, removing emitter: userId={}", userId);
            }
        });
    }
}
