package io.moer.booking.domain.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 고객 수정 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerUpdateRequest {

    @Size(min = 2, max = 50, message = "이름은 2~50자 사이여야 합니다")
    private String name;

    @Pattern(regexp = "^010-\\d{4}-\\d{4}$", message = "전화번호 형식이 올바르지 않습니다")
    private String phone;

    @Email(message = "이메일 형식이 올바르지 않습니다")
    private String email;

    private LocalDate birthDate;
    private String gender;
    private List<String> tags;
    private String memo;
}