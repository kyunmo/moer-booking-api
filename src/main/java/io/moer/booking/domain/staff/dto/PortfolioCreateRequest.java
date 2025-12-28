package io.moer.booking.domain.staff.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioCreateRequest {

    private String title;
    private String description;

    @NotBlank(message = "이미지 URL은 필수입니다")
    private String imageUrl;

    private List<String> tags;
    private Integer displayOrder;
}