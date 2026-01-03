package io.moer.booking.domain.customer.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.moer.booking.domain.customer.CustomerHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 고객 이력 응답 DTO
 */
@Getter
@Builder
@AllArgsConstructor
public class CustomerHistoryResponse {

    private Long id;
    private Long customerId;
    private Long businessId;
    private Long reservationId;
    private Long staffId;

    /**
     * 직원 이름 (JOIN 필요 - 추후 구현)
     */
    private String staffName;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate visitDate;

    /**
     * 서비스 목록 (JSONB)
     */
    private List<Map<String, Object>> services;

    private Integer totalPrice;

    /**
     * 결제 수단
     */
    private String paymentMethod;

    /**
     * 상세 정보 (JSONB)
     */
    private Map<String, Object> details;

    private String beforeImageUrl;
    private String afterImageUrl;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * Entity → DTO 변환
     */
    public static CustomerHistoryResponse from(CustomerHistory history) {
        return CustomerHistoryResponse.builder()
                .id(history.getId())
                .customerId(history.getCustomerId())
                .businessId(history.getBusinessId())
                .reservationId(history.getReservationId())
                .staffId(history.getStaffId())
                .visitDate(history.getVisitDate())
                .services(history.getServices())
                .totalPrice(history.getTotalPrice())
                .paymentMethod(history.getPaymentMethod())
                .details(history.getDetails())
                .beforeImageUrl(history.getBeforeImageUrl())
                .afterImageUrl(history.getAfterImageUrl())
                .createdAt(history.getCreatedAt())
                .build();
    }
}