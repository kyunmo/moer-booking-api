package io.moer.booking.domain.inquiry;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 문의 유형
 * DB 컬럼: inquiries.type (VARCHAR(30))
 */
@Getter
@RequiredArgsConstructor
public enum InquiryType {

    GENERAL("일반 문의"),
    FEATURE_REQUEST("기능 요청"),
    BUG_REPORT("버그 신고"),
    PARTNERSHIP("제휴 문의");

    private final String description;
}
