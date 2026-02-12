package io.moer.booking.domain.coupon.dto;

import io.moer.booking.domain.coupon.CouponUsage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "쿠폰 사용 내역 응답")
public class CouponUsageResponse {
    @Schema(description = "사용 내역 ID")
    private Long id;

    @Schema(description = "쿠폰 ID")
    private Long couponId;

    @Schema(description = "쿠폰 코드")
    private String couponCode;

    @Schema(description = "쿠폰 이름")
    private String couponName;

    @Schema(description = "사용자 ID")
    private Long userId;

    @Schema(description = "사용자 이름")
    private String userName;

    @Schema(description = "결제 ID")
    private Long paymentId;

    @Schema(description = "할인 금액")
    private Integer discountAmount;

    @Schema(description = "사용 일시")
    private LocalDateTime usedAt;

    @Schema(description = "취소 여부")
    private Boolean canceled;

    @Schema(description = "취소 일시")
    private LocalDateTime canceledAt;

    public static CouponUsageResponse from(CouponUsage usage, String couponCode, String couponName, String userName) {
        return CouponUsageResponse.builder()
            .id(usage.getId())
            .couponId(usage.getCouponId())
            .couponCode(couponCode)
            .couponName(couponName)
            .userId(usage.getUserId())
            .userName(userName)
            .paymentId(usage.getPaymentId())
            .discountAmount(usage.getDiscountAmount())
            .usedAt(usage.getUsedAt())
            .canceled(usage.isCanceled())
            .canceledAt(usage.getCanceledAt())
            .build();
    }
}
