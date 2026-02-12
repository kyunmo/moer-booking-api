package io.moer.booking.domain.coupon.dto;

import io.moer.booking.domain.coupon.CouponType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "쿠폰 생성 요청")
public class CouponCreateRequest {
    @NotBlank(message = "쿠폰 코드는 필수입니다")
    @Size(max = 50, message = "쿠폰 코드는 최대 50자입니다")
    @Schema(description = "쿠폰 코드", example = "WELCOME2026")
    private String code;

    @NotBlank(message = "쿠폰 이름은 필수입니다")
    @Size(max = 100, message = "쿠폰 이름은 최대 100자입니다")
    @Schema(description = "쿠폰 이름", example = "신규 가입 환영 쿠폰")
    private String name;

    @Schema(description = "쿠폰 설명", example = "신규 가입 고객에게 제공되는 20% 할인 쿠폰")
    private String description;

    @NotNull(message = "쿠폰 타입은 필수입니다")
    @Schema(description = "쿠폰 타입", example = "PERCENTAGE")
    private CouponType couponType;

    // 정액 할인 금액 (couponType이 FIXED_AMOUNT인 경우)
    @Min(value = 0, message = "할인 금액은 0 이상이어야 합니다")
    @Schema(description = "정액 할인 금액 (FIXED_AMOUNT 타입일 때)", example = "10000")
    private Integer discountAmount;

    // 정률 할인 비율 (couponType이 PERCENTAGE인 경우, 0~100)
    @Min(value = 0, message = "할인 비율은 0 이상이어야 합니다")
    @Max(value = 100, message = "할인 비율은 100 이하여야 합니다")
    @Schema(description = "정률 할인 비율 (PERCENTAGE 타입일 때)", example = "20")
    private Integer discountPercentage;

    // 정률 할인 시 최대 할인 금액
    @Schema(description = "최대 할인 금액 (PERCENTAGE 타입일 때)", example = "50000")
    private Integer maxDiscountAmount;

    // 최소 주문 금액 (기본값: 0)
    @Min(value = 0, message = "최소 주문 금액은 0 이상이어야 합니다")
    @Schema(description = "최소 주문 금액", example = "100000")
    private Integer minOrderAmount;

    // 최대 사용 횟수 (NULL이면 무제한)
    @Min(value = 1, message = "최대 사용 횟수는 1 이상이어야 합니다")
    @Schema(description = "최대 사용 횟수 (null이면 무제한)", example = "100")
    private Integer maxUsageCount;

    @NotNull(message = "유효 시작일은 필수입니다")
    @Schema(description = "유효 시작일", example = "2026-02-12T00:00:00")
    private LocalDateTime validFrom;

    @NotNull(message = "유효 종료일은 필수입니다")
    @Schema(description = "유효 종료일", example = "2026-03-12T23:59:59")
    private LocalDateTime validUntil;
}
