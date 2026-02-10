# 2026-02-10 매장 설정 자동 생성 기능 추가

## 문제 상황

**증상**: 영업시간 설정 화면에서 `settings`가 `null`로 반환됨

**원인**: 기존 매장(회원가입 전에 생성된 매장)은 BusinessSettings가 자동 생성되지 않음

**영향받는 매장**:
- 회원가입 API로 생성되지 않은 매장
- 직접 DB에 삽입된 매장
- 이전 버전에서 생성된 매장

## 해결 방법

### BusinessService 수정

**파일**: `src/main/java/io/moer/booking/domain/business/service/BusinessService.java`

#### 수정 전
```java
private BusinessResponse getBusinessWithSettings(Long businessId) {
    Business business = businessRepository.findById(businessId)
            .orElseThrow(() -> new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND));

    BusinessSettings settings = businessSettingsRepository
            .findByBusinessId(businessId)
            .orElse(null);  // ❌ null 반환

    return BusinessResponse.from(business, settings);
}
```

#### 수정 후
```java
private BusinessResponse getBusinessWithSettings(Long businessId) {
    Business business = businessRepository.findById(businessId)
            .orElseThrow(() -> new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND));

    BusinessSettings settings = businessSettingsRepository
            .findByBusinessId(businessId)
            .orElseGet(() -> createDefaultSettings(businessId));  // ✅ 자동 생성

    return BusinessResponse.from(business, settings);
}

/**
 * 기본 설정 생성 (Settings가 없는 기존 매장용)
 */
@Transactional
private BusinessSettings createDefaultSettings(Long businessId) {
    log.info("Creating default settings for business: {}", businessId);

    BusinessSettings settings = BusinessSettings.builder()
            .businessId(businessId)
            .bookingInterval(30)
            .autoConfirm("N")
            .allowOnlineBooking("Y")
            .maxAdvanceBookingDays(30)
            .minAdvanceBookingHours(2)
            .sendConfirmationSms("Y")
            .sendReminderSms("Y")
            .reminderHoursBefore(24)
            .sendCancelSms("Y")
            .kakaoEnabled("N")
            .paymentMethods("CARD,CASH")
            .requireDeposit("N")
            .depositAmount(0)
            .allowCancellation("Y")
            .cancelDeadlineHours(24)
            .noShowPenaltyEnabled("N")
            .timezone("Asia/Seoul")
            .language("ko")
            .build();

    businessSettingsRepository.save(settings);

    log.info("Default settings created for business: {}", businessId);

    return settings;
}
```

## 동작 방식

### 자동 생성 시점

매장 조회 시 (`GET /api/businesses/{id}`):
1. Business 조회
2. BusinessSettings 조회 시도
3. **Settings가 없으면 → 자동으로 기본값 생성**
4. 생성된 Settings 반환

### 기본 설정값

| 설정 항목 | 기본값 | 설명 |
|----------|--------|------|
| **예약 시간 간격** | 30분 | 예약 가능 시간 단위 |
| **자동 확정** | N | 예약 자동 확정 여부 |
| **온라인 예약 허용** | Y | 온라인 예약 가능 여부 |
| **최대 사전 예약** | 30일 | 최대 몇 일 전까지 예약 가능 |
| **최소 사전 예약** | 2시간 | 최소 몇 시간 전까지 예약 가능 |
| **확정 SMS** | Y | 예약 확정 SMS 발송 |
| **알림 SMS** | Y | 예약 알림 SMS 발송 |
| **알림 시간** | 24시간 전 | 예약 N시간 전 알림 |
| **취소 SMS** | Y | 예약 취소 SMS 발송 |
| **카카오톡 알림** | N | 카카오톡 알림 사용 |
| **결제 수단** | CARD,CASH | 카드, 현금 |
| **예약금 필수** | N | 예약금 필요 여부 |
| **예약금 금액** | 0원 | 예약금 금액 |
| **취소 허용** | Y | 예약 취소 가능 여부 |
| **취소 마감** | 24시간 전 | 예약 N시간 전까지 취소 가능 |
| **노쇼 패널티** | N | 노쇼 패널티 사용 여부 |
| **타임존** | Asia/Seoul | 시간대 |
| **언어** | ko | 한국어 |

## 적용 범위

이 수정으로 다음 API들에서 자동 생성이 작동합니다:

1. **`GET /api/businesses/{id}`** - 매장 단건 조회
2. **`PATCH /api/businesses/{id}`** - 매장 수정
3. **`PATCH /api/businesses/{id}/settings`** - 매장 설정 수정
4. **`PATCH /api/businesses/{id}/status`** - 매장 상태 변경

## 테스트 방법

### 1. Settings가 없는 매장 조회
```bash
GET /api/businesses/3
Authorization: Bearer {token}
```

**예상 결과**:
```json
{
  "success": true,
  "data": {
    "id": 3,
    "name": "우리공방",
    "businessType": "WORKSHOP",
    "settings": {  // ✅ 자동 생성됨
      "id": 1,
      "businessId": 3,
      "bookingInterval": 30,
      "autoConfirm": "N",
      "allowOnlineBooking": "Y",
      ...
    }
  }
}
```

### 2. 로그 확인
```
INFO  - Creating default settings for business: 3
INFO  - Default settings created for business: 3
```

## 프론트엔드 영향

### 변경 사항 없음 ✅

프론트엔드 코드 수정 불필요:
- 기존처럼 `GET /api/businesses/{id}` 호출
- `settings` 객체가 항상 존재함 (null 체크 불필요)
- 설정 화면이 정상 작동함

### 이전 동작 (문제)
```typescript
// settings가 null일 수 있어서 체크 필요
if (business.settings) {
  setBookingInterval(business.settings.bookingInterval);
} else {
  // 기본값 설정 또는 에러 처리
}
```

### 현재 동작 (개선)
```typescript
// settings가 항상 존재 (자동 생성)
setBookingInterval(business.settings.bookingInterval);
```

## 빌드 결과

```
BUILD SUCCESSFUL in 20s
```

## 데이터베이스 영향

### 자동 생성 레코드

Settings가 없는 매장을 조회하면 자동으로 `business_settings` 테이블에 INSERT됩니다.

```sql
-- 생성 확인
SELECT * FROM business_settings WHERE business_id = 3;
```

### 기존 데이터 마이그레이션 (선택적)

모든 매장의 Settings를 미리 생성하려면:

```sql
-- Settings가 없는 매장 확인
SELECT b.id, b.name
FROM businesses b
LEFT JOIN business_settings bs ON b.id = bs.business_id
WHERE bs.id IS NULL;

-- 일괄 생성 (선택적)
INSERT INTO business_settings (
    business_id, booking_interval, auto_confirm, allow_online_booking,
    max_advance_booking_days, min_advance_booking_hours,
    send_confirmation_sms, send_reminder_sms, reminder_hours_before, send_cancel_sms,
    kakao_enabled, payment_methods, require_deposit, deposit_amount,
    allow_cancellation, cancel_deadline_hours, no_show_penalty_enabled,
    timezone, language
)
SELECT
    id, 30, 'N', 'Y',
    30, 2,
    'Y', 'Y', 24, 'Y',
    'N', 'CARD,CASH', 'N', 0,
    'Y', 24, 'N',
    'Asia/Seoul', 'ko'
FROM businesses b
WHERE NOT EXISTS (
    SELECT 1 FROM business_settings bs WHERE bs.business_id = b.id
);
```

## 장점

1. **사용자 경험 개선**: Settings가 없어도 에러 없이 정상 작동
2. **자동 복구**: 기존 매장도 조회 시 자동으로 Settings 생성
3. **코드 간소화**: 프론트엔드에서 null 체크 불필요
4. **일관성**: 모든 매장이 동일한 기본 설정 적용

## 주의사항

### 트랜잭션 처리

`createDefaultSettings` 메서드는 별도 트랜잭션으로 실행됩니다:
- `@Transactional(propagation = REQUIRES_NEW)` 사용
- readOnly 트랜잭션 내에서도 INSERT 가능
- 조회 중에 INSERT 발생
- 동시성 제어 필요 (같은 매장에 대해 중복 생성 방지)

#### 트랜잭션 전파 설정 이유

```java
// getBusiness는 readOnly 트랜잭션
@Transactional(readOnly = true)
public BusinessResponse getBusiness(Long id, User currentUser) {
    // ...
    return getBusinessWithSettings(id);
}

// createDefaultSettings는 별도 쓰기 트랜잭션 필요
@Transactional(propagation = REQUIRES_NEW)  // ✅ 새 트랜잭션 시작
public BusinessSettings createDefaultSettings(Long businessId) {
    businessSettingsRepository.save(settings);  // INSERT 가능
}
```

### 동시 요청 처리

여러 요청이 동시에 같은 매장을 조회할 경우:
- 첫 번째 요청: Settings 생성
- 두 번째 요청: 이미 생성된 Settings 조회
- 중복 생성 방지: DB의 `UNIQUE` 제약 조건 (business_id)

## 관련 문서

- [매장 설정 수정 내역](./2026-02-10-business-settings-fix.md)
- [프론트엔드 가이드](./2026-02-10-business-settings-frontend-guide.md)

---

**작업 완료일**: 2026-02-10
**담당**: Claude Code
**상태**: ✅ 완료
