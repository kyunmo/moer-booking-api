package io.moer.booking.domain.inquiry;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 문의 엔티티
 * DB 테이블: inquiries
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inquiry {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private InquiryType type;
    private String content;
    private InquiryStatus status;
    private String adminNote;
    private String ipAddress;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ========================================
    // 헬퍼 메서드
    // ========================================

    public boolean isPending() {
        return InquiryStatus.PENDING.equals(this.status);
    }

    public boolean isResolved() {
        return InquiryStatus.RESOLVED.equals(this.status);
    }

    public boolean isClosed() {
        return InquiryStatus.CLOSED.equals(this.status);
    }
}
