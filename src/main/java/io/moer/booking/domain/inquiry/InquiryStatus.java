package io.moer.booking.domain.inquiry;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 문의 처리 상태
 * DB 컬럼: inquiries.status (VARCHAR(20))
 */
@Getter
@RequiredArgsConstructor
public enum InquiryStatus {

    PENDING("접수"),
    IN_PROGRESS("처리 중"),
    RESOLVED("해결됨"),
    CLOSED("종료");

    private final String description;
}
