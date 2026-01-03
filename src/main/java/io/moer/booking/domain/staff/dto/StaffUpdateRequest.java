package io.moer.booking.domain.staff.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 직원 수정 요청 DTO
 * 모든 필드 선택 (null이면 기존 값 유지)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class StaffUpdateRequest {

    /**
     * 이름
     */
    @Size(min = 2, max = 50, message = "이름은 2~50자 사이여야 합니다")
    private String name;

    /**
     * 직급
     */
    private String position;

    /**
     * 전화번호
     */
    private String phone;

    /**
     * 이메일
     */
    private String email;

    /**
     * 전문분야
     */
    private String specialty;

    /**
     * 경력 (년)
     */
    @Min(value = 0, message = "경력은 0년 이상이어야 합니다")
    private Integer careerYears;

    /**
     * 프로필 이미지 URL
     */
    private String profileImageUrl;

    /**
     * 소개글
     */
    private String introduction;
}