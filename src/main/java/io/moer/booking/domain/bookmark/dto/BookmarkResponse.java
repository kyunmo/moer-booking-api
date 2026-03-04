package io.moer.booking.domain.bookmark.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class BookmarkResponse {
    private Long id;
    private Long businessId;
    private String businessName;
    private String businessType;
    private String address;
    private String imageUrl;
    private String slug;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}
