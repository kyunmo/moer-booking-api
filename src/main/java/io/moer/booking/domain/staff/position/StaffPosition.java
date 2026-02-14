package io.moer.booking.domain.staff.position;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 직급 엔티티
 * DB 테이블: staff_positions
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffPosition {
    private Long id;
    private Long businessId;
    private String name;
    private String description;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
