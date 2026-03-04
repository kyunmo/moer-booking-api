package io.moer.booking.domain.bookmark;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerBookmark {
    private Long id;
    private Long userId;
    private Long businessId;
    private LocalDateTime createdAt;
}
