package io.moer.booking.domain.business.dto;

import io.moer.booking.domain.business.BusinessStatus;
import io.moer.booking.domain.business.BusinessType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BusinessSearchCondition {

    private String keyword;           // 매장명 검색
    private Long ownerId;             // 특정 Owner의 매장만
    private BusinessType businessType; // 업종 필터
    private BusinessStatus status;    // 상태 필터
    private int page = 1;
    private int size = 20;

    public int getOffset() {
        return (page - 1) * size;
    }
}