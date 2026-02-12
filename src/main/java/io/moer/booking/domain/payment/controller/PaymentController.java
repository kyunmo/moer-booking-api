package io.moer.booking.domain.payment.controller;

import io.moer.booking.common.dto.ApiResponse;
import io.moer.booking.common.security.CustomUserDetails;
import io.moer.booking.domain.payment.dto.PaymentCreateRequest;
import io.moer.booking.domain.payment.dto.PaymentResponse;
import io.moer.booking.domain.payment.dto.PaymentSearchCondition;
import io.moer.booking.domain.payment.service.PaymentService;
import io.moer.booking.domain.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 결제 API
 * Phase 3: 테스트용 가짜 PG (FakePGService)
 * Phase 5: 실제 Toss Payments 연동 예정
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payment", description = "결제 관리 API")
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * 결제 생성 및 처리
     * PENDING 생성 → PG 호출 → COMPLETED/FAILED
     */
    @PostMapping
    @Operation(summary = "결제 생성 및 처리", description = "구독 플랜 결제를 생성하고 즉시 처리합니다")
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PaymentCreateRequest request
    ) {
        User user = userDetails.getUser();
        PaymentResponse response = paymentService.createAndProcessPayment(user, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    /**
     * 환불 처리
     */
    @PostMapping("/{paymentId}/refund")
    @Operation(summary = "결제 환불", description = "완료된 결제를 환불 처리합니다")
    public ResponseEntity<ApiResponse<PaymentResponse>> refundPayment(
            @PathVariable Long paymentId,
            @RequestBody Map<String, String> body
    ) {
        String reason = body.getOrDefault("reason", "고객 요청");
        PaymentResponse response = paymentService.refundPayment(paymentId, reason);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 결제 내역 조회 (단건)
     */
    @GetMapping("/{paymentId}")
    @Operation(summary = "결제 내역 조회", description = "특정 결제의 상세 정보를 조회합니다")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(
            @PathVariable Long paymentId
    ) {
        PaymentResponse response = paymentService.getPayment(paymentId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 결제 내역 목록 조회
     */
    @GetMapping
    @Operation(summary = "결제 내역 목록", description = "내 매장의 결제 내역 목록을 조회합니다")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getPaymentList(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        User user = userDetails.getUser();
        PaymentSearchCondition condition = PaymentSearchCondition.builder()
                .businessId(user.getBusinessId())
                .status(status != null ? io.moer.booking.domain.payment.PaymentStatus.valueOf(status) : null)
                .page(page)
                .size(size)
                .build();

        List<PaymentResponse> payments = paymentService.getPaymentList(condition);
        return ResponseEntity.ok(ApiResponse.success(payments));
    }

    /**
     * PG 거래 ID로 결제 조회
     */
    @GetMapping("/pg/{pgTransactionId}")
    @Operation(summary = "PG 거래 ID로 조회", description = "PG사 거래 ID로 결제를 조회합니다")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByPgTransactionId(
            @PathVariable String pgTransactionId
    ) {
        PaymentResponse response = paymentService.getPaymentByPgTransactionId(pgTransactionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 최근 결제 조회
     */
    @GetMapping("/latest")
    @Operation(summary = "최근 결제 조회", description = "내 매장의 가장 최근 결제를 조회합니다")
    public ResponseEntity<ApiResponse<PaymentResponse>> getLatestPayment(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        User user = userDetails.getUser();
        PaymentResponse response = paymentService.getLatestPayment(user.getBusinessId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
