package io.moer.booking.domain.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 슬러그 사용 가능 여부 확인 응답
 */
@Getter
@Builder
@AllArgsConstructor
public class SlugCheckResponse {

    /**
     * 사용 가능 여부
     */
    private boolean available;

    /**
     * 사용 불가 시 대안 슬러그 제안
     */
    private List<String> suggestions;

    public static SlugCheckResponse available() {
        return SlugCheckResponse.builder()
                .available(true)
                .build();
    }

    public static SlugCheckResponse unavailable(List<String> suggestions) {
        return SlugCheckResponse.builder()
                .available(false)
                .suggestions(suggestions)
                .build();
    }
}
