package io.moer.booking.domain.payment.dto;

import io.moer.booking.domain.payment.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 결제 검색 조건 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentSearchCondition {
    private Long businessId;
    private PaymentStatus status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    @Builder.Default
    private Integer page = 1;

    @Builder.Default
    private Integer size = 20;

    public int getOffset() {
        return (page - 1) * size;
    }
}
