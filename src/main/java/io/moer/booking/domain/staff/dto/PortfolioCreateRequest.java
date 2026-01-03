package io.moer.booking.domain.staff.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 포트폴리오 생성 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioCreateRequest {

    /**
     * 제목 (선택)
     */
    private String title;

    /**
     * 설명 (선택)
     */
    private String description;

    /**
     * 이미지 URL (필수)
     */
    @NotBlank(message = "이미지 URL은 필수입니다")
    private String imageUrl;

    /**
     * 태그 목록 (선택)
     * 예: ["컷", "펌", "염색"]
     */
    private List<String> tags;

    /**
     * 표시 순서 (선택)
     * 미사용 (추후 구현 예정)
     */
    private Integer displayOrder;
}