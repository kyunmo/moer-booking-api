package io.moer.booking.domain.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 고객 생성 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerCreateRequest {

    @NotBlank(message = "이름은 필수입니다")
    @Size(min = 2, max = 50, message = "이름은 2~50자 사이여야 합니다")
    private String name;

    @NotBlank(message = "전화번호는 필수입니다")
    @Pattern(regexp = "^010-\\d{4}-\\d{4}$", message = "전화번호 형식이 올바르지 않습니다 (예: 010-1234-5678)")
    private String phone;

    @Email(message = "이메일 형식이 올바르지 않습니다")
    private String email;

    /**
     * 생년월일
     */
    private LocalDate birthDate;

    /**
     * 성별 (MALE/FEMALE/OTHER)
     */
    private String gender;

    /**
     * 태그 목록 (콤마 구분)
     */
    private List<String> tags;

    /**
     * 메모
     */
    private String memo;
}