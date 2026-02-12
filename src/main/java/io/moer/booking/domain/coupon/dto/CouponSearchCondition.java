package io.moer.booking.domain.coupon.dto;

import io.moer.booking.domain.coupon.CouponStatus;
import io.moer.booking.domain.coupon.CouponType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "쿠폰 검색 조건")
public class CouponSearchCondition {
    @Schema(description = "매장 ID")
    private Long businessId;

    @Schema(description = "검색 키워드 (코드 또는 이름)")
    private String keyword;

    @Schema(description = "쿠폰 타입")
    private CouponType couponType;

    @Schema(description = "쿠폰 상태")
    private CouponStatus status;

    @Schema(description = "페이지 번호", example = "1")
    @lombok.Builder.Default
    private Integer page = 1;

    @Schema(description = "페이지 크기", example = "20")
    @lombok.Builder.Default
    private Integer size = 20;

    public int getOffset() {
        return (page - 1) * size;
    }
}
