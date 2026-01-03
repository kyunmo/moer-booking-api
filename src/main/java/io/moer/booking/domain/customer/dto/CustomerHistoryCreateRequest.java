package io.moer.booking.domain.customer.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 고객 이력 생성 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerHistoryCreateRequest {

    @NotNull(message = "고객 ID는 필수입니다")
    private Long customerId;

    /**
     * 예약 ID (예약에서 자동 생성되는 경우)
     */
    private Long reservationId;

    @NotNull(message = "담당 직원 ID는 필수입니다")
    private Long staffId;

    @NotNull(message = "방문 날짜는 필수입니다")
    private LocalDate visitDate;

    /**
     * 서비스 목록 (JSONB)
     * 예: [{"id": 1, "name": "여성컷", "price": 30000}]
     */
    @NotEmpty(message = "서비스는 최소 1개 이상이어야 합니다")
    private List<Map<String, Object>> services;

    @NotNull(message = "총 금액은 필수입니다")
    private Integer totalPrice;

    /**
     * 결제 수단 (CARD/CASH/TRANSFER 등)
     */
    private String paymentMethod;

    /**
     * 상세 정보 (JSONB)
     * 예: {"color": "밝은 갈색", "length": "어깨선", "products": ["트리트먼트A"]}
     */
    private Map<String, Object> details;

    private String beforeImageUrl;
    private String afterImageUrl;
}