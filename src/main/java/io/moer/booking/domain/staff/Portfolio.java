package io.moer.booking.domain.staff;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Portfolio {

    private Long id;
    private Long staffId;
    private Long businessId;

    private String title;
    private String description;
    private String imageUrl;
    private List<String> tags;

    private Integer displayOrder;
    private Boolean isVisible;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 비즈니스 로직
    public void show() {
        this.isVisible = true;
    }

    public void hide() {
        this.isVisible = false;
    }
}