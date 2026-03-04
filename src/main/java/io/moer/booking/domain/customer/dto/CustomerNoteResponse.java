package io.moer.booking.domain.customer.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.moer.booking.domain.customer.CustomerNote;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class CustomerNoteResponse {

    private Long id;
    private Long customerId;
    private Long businessId;
    private String content;
    private Boolean isPrivate;
    private Long authorId;
    private String authorName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    public static CustomerNoteResponse from(CustomerNote note) {
        return CustomerNoteResponse.builder()
                .id(note.getId())
                .customerId(note.getCustomerId())
                .businessId(note.getBusinessId())
                .content(note.getContent())
                .isPrivate(note.getIsPrivate())
                .authorId(note.getAuthorId())
                .authorName(note.getAuthorName())
                .createdAt(note.getCreatedAt())
                .updatedAt(note.getUpdatedAt())
                .build();
    }
}
