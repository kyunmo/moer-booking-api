package io.moer.booking.domain.customer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 고객 메모 엔티티
 * DB 테이블: customer_notes
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerNote {
    private Long id;
    private Long customerId;
    private Long businessId;
    private String content;
    private Boolean isPrivate;
    private Long authorId;
    private String authorName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
