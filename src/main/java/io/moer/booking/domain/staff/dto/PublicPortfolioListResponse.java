package io.moer.booking.domain.staff.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 공개 포트폴리오 목록 응답 DTO
 * 스태프 정보와 함께 공개 포트폴리오 목록을 반환
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicPortfolioListResponse {
    private String staffName;
    private String staffPosition;
    private List<PortfolioResponse> items;
    private int totalCount;
}
