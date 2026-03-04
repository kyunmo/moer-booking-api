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
    RESERVATION_CANCEL_DEADLINE_PASSED(HttpStatus.BAD_REQUEST, "R008", "예약 취소 가능 시간이 지났습니다"),

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
    SERVICE_DUPLICATE_NAME(HttpStatus.CONFLICT, "SV007", "동일한 이름의 서비스가 이미 존재합니다"),

    // Staff Position 관련 에러 (SP001~SP004)
    STAFF_POSITION_NOT_FOUND(HttpStatus.NOT_FOUND, "SP001", "직급을 찾을 수 없습니다"),
    STAFF_POSITION_DUPLICATE_NAME(HttpStatus.CONFLICT, "SP002", "이미 존재하는 직급명입니다"),
    STAFF_POSITION_HAS_STAFFS(HttpStatus.BAD_REQUEST, "SP003", "해당 직급에 직원이 존재하여 삭제할 수 없습니다"),
    STAFF_POSITION_SORT_ORDER_INVALID(HttpStatus.BAD_REQUEST, "SP004", "정렬 순서가 올바르지 않습니다"),

    // Portfolio 관련 에러 (P001) - 추가
    PORTFOLIO_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "포트폴리오를 찾을 수 없습니다"),

    // Holiday 관련 에러 (H001) - 추가
    HOLIDAY_NOT_FOUND(HttpStatus.NOT_FOUND, "H001", "휴무일을 찾을 수 없습니다"),

    // History 관련 에러 (HI001) - 추가
    HISTORY_NOT_FOUND(HttpStatus.NOT_FOUND, "HI001", "이력을 찾을 수 없습니다"),

    // Trial 관련 에러 (TR001~TR003)
    TRIAL_EXPIRED(HttpStatus.FORBIDDEN, "TR001", "체험판 기간이 만료되었습니다"),
    TRIAL_FEATURE_RESTRICTED(HttpStatus.FORBIDDEN, "TR002", "무료 버전에서는 사용할 수 없는 기능입니다. 유료 플랜으로 업그레이드해주세요."),
    UPGRADE_REQUIRED(HttpStatus.PAYMENT_REQUIRED, "TR003", "프리미엄 업그레이드가 필요합니다"),

    // Password Reset 관련 에러 (PR001~PR003)
    RESET_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "PR001", "유효하지 않은 재설정 토큰입니다"),
    RESET_TOKEN_EXPIRED(HttpStatus.BAD_REQUEST, "PR002", "만료된 재설정 토큰입니다"),
    RESET_TOKEN_USED(HttpStatus.BAD_REQUEST, "PR003", "이미 사용된 재설정 토큰입니다"),

    // OAuth2 관련 에러 (OA001~OA004)
    OAUTH2_PROVIDER_NOT_FOUND(HttpStatus.BAD_REQUEST, "OA001", "지원하지 않는 SNS 제공자입니다"),
    OAUTH2_AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "OA002", "SNS 로그인에 실패했습니다"),
    OAUTH2_EMAIL_NOT_PROVIDED(HttpStatus.BAD_REQUEST, "OA003", "SNS에서 이메일 정보를 제공하지 않았습니다"),
    OAUTH2_ROLE_MISMATCH(HttpStatus.FORBIDDEN, "OA004", "고객 계정으로는 관리자 로그인을 할 수 없습니다. 관리자 전용 회원가입을 이용해주세요"),

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
    INVALID_BILLING_CYCLE(HttpStatus.BAD_REQUEST, "SU008", "유효하지 않은 결제 주기입니다"),

    // ============ Usage Limits (SL001 ~ SL003) ============
    STAFF_LIMIT_EXCEEDED(HttpStatus.FORBIDDEN, "SL001", "직원 수 제한에 도달했습니다. 플랜을 업그레이드하세요"),
    RESERVATION_LIMIT_EXCEEDED(HttpStatus.FORBIDDEN, "SL002", "월간 예약 수 제한에 도달했습니다. 플랜을 업그레이드하세요"),
    USAGE_SYNC_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "SL003", "사용량 동기화에 실패했습니다"),
    SERVICE_LIMIT_EXCEEDED(HttpStatus.FORBIDDEN, "SL004", "서비스 등록 수 제한에 도달했습니다. 플랜을 업그레이드하세요"),

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
    BILLING_CALCULATION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "BI002", "요금 계산 중 오류가 발생했습니다"),

    // ============ 프로필/계정 관련 (AC001 ~ AC008) ============
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "AC001", "현재 비밀번호가 일치하지 않습니다"),
    PASSWORD_CONFIRM_MISMATCH(HttpStatus.BAD_REQUEST, "AC002", "새 비밀번호와 확인 비밀번호가 일치하지 않습니다"),
    PASSWORD_POLICY_VIOLATION(HttpStatus.BAD_REQUEST, "AC003", "비밀번호는 8자 이상, 영문과 숫자를 포함해야 합니다"),
    ACCOUNT_DELETE_HAS_ACTIVE_RESERVATIONS(HttpStatus.BAD_REQUEST, "AC004", "진행 중인 예약이 있어 탈퇴할 수 없습니다"),
    ACCOUNT_ALREADY_DELETED(HttpStatus.BAD_REQUEST, "AC005", "이미 탈퇴된 계정입니다"),
    LAST_LOGIN_METHOD_CANNOT_REMOVE(HttpStatus.BAD_REQUEST, "AC006", "마지막 로그인 수단은 해제할 수 없습니다"),
    SNS_ACCOUNT_NOT_CONNECTED(HttpStatus.NOT_FOUND, "AC007", "연결되지 않은 SNS 계정입니다"),
    SAME_PASSWORD(HttpStatus.BAD_REQUEST, "AC008", "새 비밀번호는 현재 비밀번호와 달라야 합니다"),

    // ============ 파일 업로드 관련 (FI001 ~ FI005) ============
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "FI001", "파일 업로드에 실패했습니다"),
    FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "FI002", "파일 크기가 제한을 초과했습니다"),
    UNSUPPORTED_FILE_TYPE(HttpStatus.BAD_REQUEST, "FI003", "지원하지 않는 파일 형식입니다"),
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "FI004", "파일을 찾을 수 없습니다"),
    FILE_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "FI005", "파일 삭제에 실패했습니다"),

    // ============ 알림 관련 (NT001 ~ NT003) ============
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NT001", "알림을 찾을 수 없습니다"),
    NOTIFICATION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "NT002", "해당 알림에 접근 권한이 없습니다"),

    // ============ 알림 발송 로그 관련 (NL001) ============
    NOTIFICATION_LOG_NOT_FOUND(HttpStatus.NOT_FOUND, "NL001", "알림 발송 이력을 찾을 수 없습니다"),

    // ============ 스태프 스케줄 관련 (SS001 ~ SS004) ============
    SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "SS001", "근무 스케줄을 찾을 수 없습니다"),
    INVALID_SCHEDULE_TIME(HttpStatus.BAD_REQUEST, "SS002", "근무 시간이 올바르지 않습니다"),
    STAFF_NOT_WORKING_DAY(HttpStatus.BAD_REQUEST, "SS003", "해당 직원의 근무일이 아닙니다"),
    NO_AVAILABLE_SLOTS(HttpStatus.NOT_FOUND, "SS004", "예약 가능한 시간이 없습니다"),

    // ============ 포트폴리오 확장 (P002) ============
    PORTFOLIO_IMAGE_REQUIRED(HttpStatus.BAD_REQUEST, "P002", "포트폴리오 이미지는 필수입니다"),

    // ============ 온보딩 관련 (OB001 ~ OB002) ============
    ONBOARDING_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "OB001", "이미 온보딩이 완료되었습니다"),
    ONBOARDING_ALREADY_SKIPPED(HttpStatus.BAD_REQUEST, "OB002", "이미 온보딩을 건너뛰었습니다"),

    // ============ 고객 예약 Public (BK001 ~ BK010) ============
    BOOKING_TIME_UNAVAILABLE(HttpStatus.CONFLICT, "BK001", "해당 시간대에 예약할 수 없습니다"),
    BOOKING_OUTSIDE_HOURS(HttpStatus.BAD_REQUEST, "BK002", "영업시간 외에는 예약할 수 없습니다"),
    BOOKING_HOLIDAY(HttpStatus.BAD_REQUEST, "BK003", "휴무일에는 예약할 수 없습니다"),
    BOOKING_STAFF_UNAVAILABLE(HttpStatus.BAD_REQUEST, "BK004", "해당 스태프가 근무하지 않는 시간입니다"),
    CANCEL_DEADLINE_EXCEEDED(HttpStatus.BAD_REQUEST, "BK005", "취소 가능 시간이 지났습니다"),
    RESERVATION_ALREADY_CANCELLED(HttpStatus.BAD_REQUEST, "BK006", "이미 취소된 예약입니다"),
    ONLINE_BOOKING_DISABLED(HttpStatus.FORBIDDEN, "BK007", "온라인 예약이 비활성화되어 있습니다"),
    ADVANCE_BOOKING_EXCEEDED(HttpStatus.BAD_REQUEST, "BK008", "예약 가능 기간을 초과했습니다"),
    MIN_ADVANCE_TIME_NOT_MET(HttpStatus.BAD_REQUEST, "BK009", "최소 사전 예약 시간을 충족하지 않습니다"),
    RESERVATION_PHONE_MISMATCH(HttpStatus.FORBIDDEN, "BK010", "전화번호가 일치하지 않습니다"),

    // ============ 슬러그 (BS001 ~ BS003) ============
    SLUG_ALREADY_EXISTS(HttpStatus.CONFLICT, "BS001", "이미 사용 중인 슬러그입니다"),
    SLUG_INVALID_FORMAT(HttpStatus.BAD_REQUEST, "BS002", "슬러그 형식이 올바르지 않습니다"),
    SLUG_RESERVED_WORD(HttpStatus.BAD_REQUEST, "BS003", "사용할 수 없는 슬러그입니다"),

    // ============ 리뷰 (RV001 ~ RV006) ============
    REVIEW_RESERVATION_MISMATCH(HttpStatus.BAD_REQUEST, "RV001", "예약 정보가 일치하지 않습니다"),
    REVIEW_NOT_COMPLETED(HttpStatus.BAD_REQUEST, "RV002", "완료된 예약만 리뷰를 작성할 수 있습니다"),
    REVIEW_ALREADY_EXISTS(HttpStatus.CONFLICT, "RV003", "이미 리뷰가 작성된 예약입니다"),
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "RV004", "리뷰를 찾을 수 없습니다"),
    REVIEW_ALREADY_REPLIED(HttpStatus.BAD_REQUEST, "RV005", "이미 답변이 등록된 리뷰입니다"),
    REVIEW_ALREADY_DELETED(HttpStatus.BAD_REQUEST, "RV006", "이미 삭제된 리뷰입니다"),

    // ============ 고객 인증 (CP001 ~ CP004) ============
    CUSTOMER_PHONE_REQUIRED(HttpStatus.BAD_REQUEST, "CP001", "예약을 위해 전화번호 등록이 필요합니다"),
    CUSTOMER_ROLE_REQUIRED(HttpStatus.FORBIDDEN, "CP002", "고객 권한이 필요합니다"),
    CUSTOMER_RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "CP003", "고객의 예약을 찾을 수 없습니다"),
    CUSTOMER_REVIEW_UNAUTHORIZED(HttpStatus.FORBIDDEN, "CP004", "본인의 예약에 대해서만 리뷰를 작성할 수 있습니다"),

    // ============ 통계 (ST001 ~ ST005) ============
    STATISTICS_INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "ST001", "시작일은 종료일보다 이전이어야 합니다"),
    STATISTICS_DATE_RANGE_EXCEEDED(HttpStatus.BAD_REQUEST, "ST002", "조회 가능 기간은 최대 1년입니다"),
    STATISTICS_INVALID_GROUP_BY(HttpStatus.BAD_REQUEST, "ST003", "유효하지 않은 집계 단위입니다 (daily/weekly/monthly)"),
    STATISTICS_STAFF_NOT_FOUND(HttpStatus.NOT_FOUND, "ST004", "통계 조회 대상 직원을 찾을 수 없습니다"),
    STATISTICS_CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "ST005", "통계 조회 대상 카테고리를 찾을 수 없습니다"),

    // ============ 문의하기 (IQ001) ============
    INQUIRY_RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "IQ001", "너무 많은 문의를 보내셨습니다. 잠시 후 다시 시도해주세요."),

    // ============ Payment Cancel (PM001 ~ PM002) ============
    PAYMENT_ALREADY_CANCELLED(HttpStatus.BAD_REQUEST, "PM001", "이미 취소된 결제입니다"),
    PAYMENT_CANNOT_CANCEL(HttpStatus.BAD_REQUEST, "PM002", "취소할 수 없는 결제 상태입니다"),

    // ============ 즐겨찾기 (BM001 ~ BM003) ============
    BOOKMARK_NOT_FOUND(HttpStatus.NOT_FOUND, "BM001", "즐겨찾기를 찾을 수 없습니다"),
    BOOKMARK_ALREADY_EXISTS(HttpStatus.CONFLICT, "BM002", "이미 즐겨찾기에 추가된 매장입니다"),
    BOOKMARK_ACCESS_DENIED(HttpStatus.FORBIDDEN, "BM003", "즐겨찾기에 접근 권한이 없습니다"),

    // ============ 리뷰 이미지 (RI001 ~ RI003) ============
    REVIEW_IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "RI001", "리뷰 이미지를 찾을 수 없습니다"),
    REVIEW_IMAGE_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "RI002", "리뷰당 최대 5개의 이미지만 등록할 수 있습니다"),
    REVIEW_UPDATE_DENIED(HttpStatus.FORBIDDEN, "RI003", "본인의 리뷰만 수정할 수 있습니다"),

    // ============ 고객 세그멘테이션 (SG001 ~ SG002) ============
    INVALID_SEGMENT_TYPE(HttpStatus.BAD_REQUEST, "SG001", "유효하지 않은 세그먼트 타입입니다"),
    SEGMENT_QUERY_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "SG002", "세그먼트 조회에 실패했습니다"),

    // ============ 공지 방송 (BC001 ~ BC003) ============
    BROADCAST_NOT_FOUND(HttpStatus.NOT_FOUND, "BC001", "공지를 찾을 수 없습니다"),
    BROADCAST_ALREADY_SENT(HttpStatus.BAD_REQUEST, "BC002", "이미 발송된 공지입니다"),
    INVALID_BROADCAST_TARGET(HttpStatus.BAD_REQUEST, "BC003", "유효하지 않은 발송 대상입니다"),

    // 서비스 이미지 (IMG001~IMG005)
    SERVICE_IMAGE_UNSUPPORTED_FORMAT(HttpStatus.BAD_REQUEST, "IMG001", "지원하지 않는 파일 형식입니다. (jpg, jpeg, png, webp만 허용)"),
    SERVICE_IMAGE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "IMG002", "파일 크기가 초과되었습니다. (최대 10MB)"),
    SERVICE_IMAGE_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "IMG003", "서비스 이미지는 최대 3장까지 등록 가능합니다"),
    SERVICE_IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "IMG004", "이미지를 찾을 수 없습니다"),
    SERVICE_IMAGE_ORDER_MISMATCH(HttpStatus.BAD_REQUEST, "IMG005", "이미지 ID 목록이 서비스의 이미지와 일치하지 않습니다"),

    // 알림 발송 (NTF001~NTF003)
    NOTIFICATION_TARGET_EMPTY(HttpStatus.BAD_REQUEST, "NTF001", "발송 대상이 없습니다"),
    NOTIFICATION_INVALID_CHANNEL(HttpStatus.BAD_REQUEST, "NTF002", "유효하지 않은 발송 채널입니다"),
    KAKAO_CHANNEL_VERIFICATION_FAILED(HttpStatus.BAD_REQUEST, "NTF003", "카카오 채널 ID 인증에 실패했습니다"),

    // 고객 CRM (CRM001~CRM005)
    CUSTOMER_NOTE_NOT_FOUND(HttpStatus.NOT_FOUND, "CRM001", "메모를 찾을 수 없습니다"),
    CUSTOMER_TAG_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "CRM002", "태그는 최대 10개까지 등록 가능합니다"),
    CUSTOMER_TAG_LENGTH_EXCEEDED(HttpStatus.BAD_REQUEST, "CRM003", "태그 이름은 최대 20자까지 입력 가능합니다"),
    CUSTOMER_MERGE_PRIMARY_CONFLICT(HttpStatus.BAD_REQUEST, "CRM004", "주 고객 ID가 병합 목록에 포함될 수 없습니다"),
    CUSTOMER_MERGE_EMPTY(HttpStatus.BAD_REQUEST, "CRM005", "병합할 고객이 없습니다"),

    // 플랫폼 통계 (STAT001)
    PLATFORM_STATS_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "STAT001", "통계 데이터를 일시적으로 사용할 수 없습니다"),

    // 도움말 (PUB001)
    HELP_ARTICLE_NOT_FOUND(HttpStatus.NOT_FOUND, "PUB001", "도움말 콘텐츠를 찾을 수 없습니다"),

    // 예약 일정 변경 (RS001~RS004)
    RESCHEDULE_TIME_CONFLICT(HttpStatus.CONFLICT, "RS001", "해당 시간대에 직원이 이미 예약되어 있습니다"),
    RESCHEDULE_OUTSIDE_HOURS(HttpStatus.BAD_REQUEST, "RS002", "직원의 근무 시간 외의 시간입니다"),
    RESCHEDULE_HOLIDAY(HttpStatus.BAD_REQUEST, "RS003", "휴무일에는 예약을 등록할 수 없습니다"),
    RESCHEDULE_INVALID_STATUS(HttpStatus.BAD_REQUEST, "RS004", "취소/완료된 예약은 일정을 변경할 수 없습니다"),

    // 직원 스케줄 조회 (SCH001~SCH002)
    SCHEDULE_DATE_RANGE_EXCEEDED(HttpStatus.BAD_REQUEST, "SCH001", "조회 기간이 최대 31일을 초과합니다"),
    SCHEDULE_INVALID_DATE_ORDER(HttpStatus.BAD_REQUEST, "SCH002", "시작일이 종료일보다 늦습니다"),

    // 알림 발송 한도 (NTF004)
    NOTIFICATION_SEND_LIMIT_EXCEEDED(HttpStatus.FORBIDDEN, "NTF004", "이번 달 알림 발송 한도를 초과했습니다"),

    // CSV 내보내기 (EX001~EX002)
    EXPORT_PLAN_RESTRICTED(HttpStatus.FORBIDDEN, "EX001", "해당 플랜에서는 CSV 내보내기를 사용할 수 없습니다"),
    EXPORT_RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "EX002", "내보내기 요청이 너무 많습니다. 잠시 후 다시 시도해주세요");

    private final HttpStatus status;
    private final String code;
    private final String message;
}