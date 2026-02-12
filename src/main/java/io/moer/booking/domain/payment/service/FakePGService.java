package io.moer.booking.domain.payment.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 테스트용 가짜 PG 서비스
 * Phase 5에서 실제 Toss Payments로 교체 예정
 */
@Slf4j
@Service
public class FakePGService {

    /**
     * 결제 요청 (시뮬레이션)
     * 90% 확률로 성공, 10% 확률로 실패
     *
     * @param amount 결제 금액
     * @param paymentMethod 결제 수단
     * @return PG 응답 (transactionId, paymentKey, status)
     */
    public Map<String, Object> requestPayment(Integer amount, String paymentMethod) {
        log.info("🎭 FakePG: 결제 요청 - 금액: {}원, 수단: {}", amount, paymentMethod);

        // 랜덤으로 성공/실패 (90% 성공률)
        boolean success = Math.random() < 0.9;

        Map<String, Object> response = new HashMap<>();
        response.put("transactionId", "FAKE_TXN_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        response.put("paymentKey", "FAKE_KEY_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        response.put("status", success ? "COMPLETED" : "FAILED");
        response.put("amount", amount);
        response.put("method", paymentMethod);
        response.put("provider", "FAKE_PG");

        if (!success) {
            response.put("failReason", "카드 한도 초과 (테스트 실패)");
        }

        log.info("🎭 FakePG: 결제 응답 - 상태: {}, TXN: {}",
            success ? "성공" : "실패", response.get("transactionId"));

        return response;
    }

    /**
     * 환불 요청 (시뮬레이션)
     * 항상 성공
     *
     * @param transactionId PG 거래 ID
     * @param amount 환불 금액
     * @param reason 환불 사유
     * @return PG 환불 응답
     */
    public Map<String, Object> requestRefund(String transactionId, Integer amount, String reason) {
        log.info("🎭 FakePG: 환불 요청 - TXN: {}, 금액: {}원, 사유: {}", transactionId, amount, reason);

        // 항상 성공
        Map<String, Object> response = new HashMap<>();
        response.put("transactionId", transactionId);
        response.put("refundStatus", "COMPLETED");
        response.put("refundedAmount", amount);
        response.put("refundReason", reason);
        response.put("provider", "FAKE_PG");

        log.info("🎭 FakePG: 환불 완료 - TXN: {}, 금액: {}원", transactionId, amount);

        return response;
    }

    /**
     * 결제 상태 조회 (시뮬레이션)
     *
     * @param transactionId PG 거래 ID
     * @return PG 응답
     */
    public Map<String, Object> getPaymentStatus(String transactionId) {
        log.info("🎭 FakePG: 결제 상태 조회 - TXN: {}", transactionId);

        Map<String, Object> response = new HashMap<>();
        response.put("transactionId", transactionId);
        response.put("status", "COMPLETED");
        response.put("provider", "FAKE_PG");

        return response;
    }
}
