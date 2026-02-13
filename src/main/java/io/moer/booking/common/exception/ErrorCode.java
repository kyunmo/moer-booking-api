package io.moer.booking.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통 에러 (C001~C006)
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력값입니다"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C002", "지원하지 않는 HTTP 메서드입니다"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C003", "서버 내부 오류가 발생했습니다"),
    INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "C004", "잘못된 타입입니다"),
    ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "C005", "엔티티를 찾을 수 없습니다"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "C006", "접근 권한이 없습니다"),

    // 인증/권한 관련 에러 (A001~A005)
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A001", "인증이 필요합니다"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A002", "유효하지 않은 토큰입니다"),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "A003", "만료된 토큰입니다"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "A004", "아이디 또는 비밀번호가 올바르지 않습니다"),
    TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "A005", "토큰을 찾을 수 없습니다"),  // 추가

    // 사용자 관련 에러 (U001~U004)
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "사용자를 찾을 수 없습니다"),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "U002", "이미 사용 중인 이메일입니다"),
    DUPLICATE_PHONE(HttpStatus.CONFLICT, "U003", "이미 사용 중인 전화번호입니다"),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "U004", "비밀번호가 일치하지 않습니다"),

    // 비즈니스 관련 에러 (B001~B003)
    BUSINESS_NOT_FOUND(HttpStatus.NOT_FOUND, "B001", "매장을 찾을 수 없습니다"),
    BUSINESS_ALREADY_EXISTS(HttpStatus.CONFLICT, "B002", "이미 등록된 매장입니다"),
    BUSINESS_ACCESS_DENIED(HttpStatus.FORBIDDEN, "B003", "해당 매장에 접근 권한이 없습니다"),

    // 예약 관련 에러 (R001~R007)
    RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "R001", "예약을 찾을 수 없습니다"),
    RESERVATION_TIME_CONFLICT(HttpStatus.CONFLICT, "R002", "이미 예약된 시간입니다"),
    RESERVATION_ALREADY_CONFIRMED(HttpStatus.BAD_REQUEST, "R003", "이미 확정된 예약입니다"),
    RESERVATION_CANCELLED(HttpStatus.BAD_REQUEST, "R004", "취소된 예약입니다"),
    RESERVATION_PAST_DATE(HttpStatus.BAD_REQUEST, "R005", "과거 날짜는 예약할 수 없습니다"),  // 추가
    RESERVATION_HOLIDAY(HttpStatus.BAD_REQUEST, "R006", "해당 날짜는 휴무일입니다"),  // 추가
    RESERVATION_INVALID_STATUS(HttpStatus.BAD_REQUEST, "R007", "잘못된 예약 상태입니다"),  // 추가

    // Staff 관련 에러 (S001~S002) - 추가
    STAFF_NOT_FOUND(HttpStatus.NOT_FOUND, "S001", "직원을 찾을 수 없습니다"),
    STAFF_ACCESS_DENIED(HttpStatus.FORBIDDEN, "S002", "해당 직원 정보에 접근 권한이 없습니다"),

    // Customer 관련 에러 (CU001~CU002) - 추가
    CUSTOMER_NOT_FOUND(HttpStatus.NOT_FOUND, "CU001", "고객을 찾을 수 없습니다"),
    DUPLICATE_CUSTOMER_PHONE(HttpStatus.CONFLICT, "CU002", "이미 등록된 전화번호입니다"),

    // Service 관련 에러 (SV001~SV006) - 추가
    SERVICE_NOT_FOUND(HttpStatus.NOT_FOUND, "SV001", "서비스를 찾을 수 없습니다"),
    SERVICE_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "SV002", "이용할 수 없는 서비스입니다"),
    SERVICE_CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "SV003", "서비스 카테고리를 찾을 수 없습니다"),
    SERVICE_CATEGORY_DUPLICATE_NAME(HttpStatus.CONFLICT, "SV004", "이미 존재하는 카테고리명입니다"),
    SERVICE_CATEGORY_HAS_SERVICES(HttpStatus.BAD_REQUEST, "SV005", "해당 카테고리에 서비스가 존재하여 삭제할 수 없습니다"),
    SERVICE_CATEGORY_SORT_ORDER_INVALID(HttpStatus.BAD_REQUEST, "SV006", "정렬 순서가 올바르지 않습니다"),

    // Portfolio 관련 에러 (P001) - 추가
    PORTFOLIO_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "포트폴리오를 찾을 수 없습니다"),

    // Holiday 관련 에러 (H001) - 추가
    HOLIDAY_NOT_FOUND(HttpStatus.NOT_FOUND, "H001", "휴무일을 찾을 수 없습니다"),

    // History 관련 에러 (HI001) - 추가
    HISTORY_NOT_FOUND(HttpStatus.NOT_FOUND, "HI001", "이력을 찾을 수 없습니다"),

    // Trial 관련 에러 (TR001~TR003)
    TRIAL_EXPIRED(HttpStatus.FORBIDDEN, "TR001", "체험판 기간이 만료되었습니다"),
    TRIAL_FEATURE_RESTRICTED(HttpStatus.FORBIDDEN, "TR002", "체험판에서는 사용할 수 없는 기능입니다"),
    UPGRADE_REQUIRED(HttpStatus.PAYMENT_REQUIRED, "TR003", "프리미엄 업그레이드가 필요합니다"),

    // Password Reset 관련 에러 (PR001~PR003)
    RESET_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "PR001", "유효하지 않은 재설정 토큰입니다"),
    RESET_TOKEN_EXPIRED(HttpStatus.BAD_REQUEST, "PR002", "만료된 재설정 토큰입니다"),
    RESET_TOKEN_USED(HttpStatus.BAD_REQUEST, "PR003", "이미 사용된 재설정 토큰입니다"),

    // OAuth2 관련 에러 (OA001~OA003)
    OAUTH2_PROVIDER_NOT_FOUND(HttpStatus.BAD_REQUEST, "OA001", "지원하지 않는 SNS 제공자입니다"),
    OAUTH2_AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "OA002", "SNS 로그인에 실패했습니다"),
    OAUTH2_EMAIL_NOT_PROVIDED(HttpStatus.BAD_REQUEST, "OA003", "SNS에서 이메일 정보를 제공하지 않았습니다"),

    // Super Admin 관련 (SA001~SA010)
    SUPER_ADMIN_REQUIRED(HttpStatus.FORBIDDEN, "SA001", "슈퍼 관리자 권한이 필요합니다"),
    SUPER_ADMIN_CANNOT_BE_DELETED(HttpStatus.BAD_REQUEST, "SA002", "슈퍼 관리자는 삭제할 수 없습니다"),
    SUPER_ADMIN_ONLY_ACTION(HttpStatus.FORBIDDEN, "SA003", "슈퍼 관리자만 수행할 수 있는 작업입니다"),

    // Audit Log 관련 (AL001~AL010)
    AUDIT_LOG_NOT_FOUND(HttpStatus.NOT_FOUND, "AL001", "감사 로그를 찾을 수 없습니다"),

    // ============ Subscription (SU001 ~ SU007) ============
    SUBSCRIPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "SU001", "구독 정보를 찾을 수 없습니다"),
    SUBSCRIPTION_EXPIRED(HttpStatus.FORBIDDEN, "SU002", "구독이 만료되었습니다"),
    TRIAL_ALREADY_USED(HttpStatus.BAD_REQUEST, "SU003", "이미 무료 체험을 사용했습니다"),
    SAME_PLAN(HttpStatus.BAD_REQUEST, "SU004", "이미 동일한 플랜입니다"),
    DOWNGRADE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "SU005", "현재 사용량이 다운그레이드할 플랜의 제한을 초과합니다"),
    SUBSCRIPTION_UPDATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "SU006", "구독 변경에 실패했습니다"),
    INVALID_SUBSCRIPTION_PLAN(HttpStatus.BAD_REQUEST, "SU007", "유효하지 않은 구독 플랜입니다"),

    // ============ Usage Limits (SL001 ~ SL003) ============
    STAFF_LIMIT_EXCEEDED(HttpStatus.FORBIDDEN, "SL001", "직원 수 제한에 도달했습니다. 플랜을 업그레이드하세요"),
    RESERVATION_LIMIT_EXCEEDED(HttpStatus.FORBIDDEN, "SL002", "월간 예약 수 제한에 도달했습니다. 플랜을 업그레이드하세요"),
    USAGE_SYNC_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "SL003", "사용량 동기화에 실패했습니다"),

    // ============ Payment (PA001 ~ PA010) ============
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "PA001", "결제 정보를 찾을 수 없습니다"),
    PAYMENT_FAILED(HttpStatus.BAD_REQUEST, "PA002", "결제에 실패했습니다"),
    PAYMENT_CANCELLED(HttpStatus.BAD_REQUEST, "PA003", "결제가 취소되었습니다"),
    INVALID_PAYMENT_AMOUNT(HttpStatus.BAD_REQUEST, "PA004", "결제 금액이 올바르지 않습니다"),
    INVALID_WEBHOOK_SIGNATURE(HttpStatus.FORBIDDEN, "PA005", "유효하지 않은 웹훅 서명입니다"),
    REFUND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "PA006", "환불 처리에 실패했습니다"),
    PAYMENT_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "PA007", "이미 완료된 결제입니다"),
    PAYMENT_CANNOT_REFUND(HttpStatus.BAD_REQUEST, "PA008", "환불할 수 없는 결제입니다"),
    PG_CONNECTION_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "PA009", "PG사 연결에 실패했습니다"),
    PG_TRANSACTION_FAILED(HttpStatus.BAD_REQUEST, "PA010", "PG사 거래 처리에 실패했습니다"),

    // ============ Coupon (CO001 ~ CO009) ============
    COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, "CO001", "쿠폰을 찾을 수 없습니다"),
    COUPON_EXPIRED(HttpStatus.BAD_REQUEST, "CO002", "만료된 쿠폰입니다"),
    COUPON_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "CO003", "쿠폰 사용 한도에 도달했습니다"),
    COUPON_ALREADY_USED(HttpStatus.BAD_REQUEST, "CO004", "이미 사용한 쿠폰입니다"),
    COUPON_NOT_APPLICABLE(HttpStatus.BAD_REQUEST, "CO005", "이 플랜에는 적용할 수 없는 쿠폰입니다"),
    COUPON_MIN_AMOUNT_NOT_MET(HttpStatus.BAD_REQUEST, "CO006", "최소 구매 금액을 충족하지 않습니다"),
    INVALID_COUPON_CODE(HttpStatus.BAD_REQUEST, "CO007", "유효하지 않은 쿠폰 코드입니다"),
    COUPON_NOT_ACTIVE(HttpStatus.BAD_REQUEST, "CO008", "비활성화된 쿠폰입니다"),
    COUPON_DUPLICATE_CODE(HttpStatus.CONFLICT, "CO009", "이미 존재하는 쿠폰 코드입니다"),

    // ============ Billing (BI001 ~ BI002) ============
    BILLING_INFO_NOT_FOUND(HttpStatus.NOT_FOUND, "BI001", "결제 정보를 찾을 수 없습니다"),
    BILLING_CALCULATION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "BI002", "요금 계산 중 오류가 발생했습니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}