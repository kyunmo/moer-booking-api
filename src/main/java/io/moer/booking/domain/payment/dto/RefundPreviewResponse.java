package io.moer.booking.domain.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 환불 미리보기 응답 DTO
 */
@Getter
@Builder
@AllArgsConstructor
public class RefundPreviewResponse {
    private Long paymentId;
    private Integer originalAmount;
    private Integer refundAmount;
    private Long usedDays;
    private Long remainingDays;
    private Long totalDays;
    private Integer usagePercent;
    private Boolean isFullRefund;
    private String formula;
}
