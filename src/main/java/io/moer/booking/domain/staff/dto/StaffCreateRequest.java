package io.moer.booking.domain.staff.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 직원 생성 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class StaffCreateRequest {

    /**
     * User ID (선택)
     * 시스템 사용자와 연결할 때 사용
     */
    private Long userId;

    /**
     * 이름 (필수)
     */
    @NotBlank(message = "이름은 필수입니다")
    @Size(min = 2, max = 50, message = "이름은 2~50자 사이여야 합니다")
    private String name;

    /**
     * 직급 (선택) - 텍스트
     * 예: 원장, 실장, 디자이너, 수석
     */
    private String position;

    /**
     * 직급 ID (선택)
     * staff_positions 테이블 참조. 설정 시 position 텍스트 자동 채움.
     */
    private Long positionId;

    /**
     * 전화번호 (선택)
     */
    private String phone;

    /**
     * 이메일 (선택)
     */
    private String email;

    /**
     * 전문분야 (선택)
     * 예: 펌, 컬러, 남성컷
     */
    private String specialty;

    /**
     * 경력 (년) (선택)
     */
    @Min(value = 0, message = "경력은 0년 이상이어야 합니다")
    private Integer careerYears;

    /**
     * 프로필 이미지 URL (선택)
     */
    private String profileImageUrl;

    /**
     * 소개글 (선택)
     */
    private String introduction;
}