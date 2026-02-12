package io.moer.booking.domain.coupon.dto;

import io.moer.booking.domain.coupon.Coupon;
import io.moer.booking.domain.coupon.CouponStatus;
import io.moer.booking.domain.coupon.CouponType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "쿠폰 응답")
public class CouponResponse {
    @Schema(description = "쿠폰 ID")
    private Long id;

    @Schema(description = "매장 ID")
    private Long businessId;

    @Schema(description = "쿠폰 코드")
    private String code;

    @Schema(description = "쿠폰 이름")
    private String name;

    @Schema(description = "쿠폰 설명")
    private String description;

    @Schema(description = "쿠폰 타입")
    private CouponType couponType;

    @Schema(description = "정액 할인 금액")
    private Integer discountAmount;

    @Schema(description = "정률 할인 비율")
    private Integer discountPercentage;

    @Schema(description = "최대 할인 금액")
    private Integer maxDiscountAmount;

    @Schema(description = "최소 주문 금액")
    private Integer minOrderAmount;

    @Schema(description = "최대 사용 횟수")
    private Integer maxUsageCount;

    @Schema(description = "현재 사용 횟수")
    private Integer currentUsageCount;

    @Schema(description = "남은 사용 횟수")
    private Integer remainingUsageCount;

    @Schema(description = "유효 시작일")
    private LocalDateTime validFrom;

    @Schema(description = "유효 종료일")
    private LocalDateTime validUntil;

    @Schema(description = "쿠폰 상태")
    private CouponStatus status;

    @Schema(description = "만료 여부")
    private Boolean isExpired;

    @Schema(description = "생성일")
    private LocalDateTime createdAt;

    public static CouponResponse from(Coupon coupon) {
        return CouponResponse.builder()
            .id(coupon.getId())
            .businessId(coupon.getBusinessId())
            .code(coupon.getCode())
            .name(coupon.getName())
            .description(coupon.getDescription())
            .couponType(coupon.getCouponType())
            .discountAmount(coupon.getDiscountAmount())
            .discountPercentage(coupon.getDiscountPercentage())
            .maxDiscountAmount(coupon.getMaxDiscountAmount())
            .minOrderAmount(coupon.getMinOrderAmount())
            .maxUsageCount(coupon.getMaxUsageCount())
            .currentUsageCount(coupon.getCurrentUsageCount())
            .remainingUsageCount(coupon.getRemainingUsageCount())
            .validFrom(coupon.getValidFrom())
            .validUntil(coupon.getValidUntil())
            .status(coupon.getStatus())
            .isExpired(coupon.isExpired())
            .createdAt(coupon.getCreatedAt())
            .build();
    }
}
