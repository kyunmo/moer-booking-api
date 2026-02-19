package io.moer.booking.domain.auth.dto;

import io.moer.booking.domain.business.BusinessType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원가입 요청 DTO (사용자 + 매장 통합)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    // === 사용자 정보 ===
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String email;

    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다")
    private String password;

    @NotBlank(message = "이름은 필수입니다")
    private String name;

    private String phone;

    // === 매장 정보 ===
    @NotBlank(message = "매장명은 필수입니다")
    private String businessName;

    @NotNull(message = "업종은 필수입니다")
    private BusinessType businessType;

    // === 구독 정보 (선택) ===
    /**
     * 선택한 구독 플랜 (기본값: BASIC)
     * 프론트엔드에서 선택하지 않으면 BASIC 플랜으로 가입
     */
    private String selectedPlan; // "FREE", "BASIC"

    /**
     * 선택한 플랜을 Enum으로 변환
     * 기본값: BASIC
     */
    public io.moer.booking.domain.business.SubscriptionPlan getSubscriptionPlan() {
        if (selectedPlan == null || selectedPlan.isEmpty()) {
            return io.moer.booking.domain.business.SubscriptionPlan.BASIC;
        }
        try {
            return io.moer.booking.domain.business.SubscriptionPlan.valueOf(selectedPlan.toUpperCase());
        } catch (IllegalArgumentException e) {
            return io.moer.booking.domain.business.SubscriptionPlan.BASIC;
        }
    }
}