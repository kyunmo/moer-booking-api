# Trial(30일 체험) → 유료 전환 시스템 — 백엔드 응답

> 작성일: 2026-02-20
> 프론트엔드 요청(`docs/backend-trial-system-request.md`)에 대한 백엔드 응답

---

## 1. GET /api/subscription 응답 필드 — 확인 완료

모든 필드가 구현되어 있으며 정상 반환됩니다.

```json
{
  "plan": "FREE",
  "status": "TRIAL",
  "planDescription": "무료",
  "monthlyPrice": 0,
  "yearlyPrice": 0,
  "billingCycle": "MONTHLY",
  "maxStaff": 1,
  "maxMonthlyReservations": 30,
  "maxServices": 10,
  "showAds": true,
  "isTrialActive": true,
  "trialStartedAt": "2026-02-01T10:00:00",
  "trialEndsAt": "2026-03-03T10:00:00",
  "daysUntilTrialEnd": 11,
  "subscriptionStartedAt": null,
  "nextBillingDate": null,
  "currentStaffCount": 1,
  "currentMonthReservationCount": 12,
  "canUseService": true,
  "canAddStaff": false,
  "canCreateReservation": true
}
```

### 필드 상세

| 필드 | 타입 | 설명 |
|------|------|------|
| `plan` | `"FREE" \| "BASIC"` | 현재 구독 플랜 |
| `status` | `"TRIAL" \| "ACTIVE" \| "EXPIRED" \| "CANCELED" \| "SUSPENDED"` | 구독 상태 |
| `planDescription` | `String` | 플랜 설명 (FREE→"무료", BASIC→"유료") |
| `monthlyPrice` | `Integer` | 월간 가격 (원) |
| `yearlyPrice` | `Integer` | 연간 가격 (원) |
| `billingCycle` | `"MONTHLY" \| "YEARLY"` | 결제 주기 |
| `maxStaff` | `Integer` | 최대 직원 수 (-1=무제한) |
| `maxMonthlyReservations` | `Integer` | 월간 최대 예약 수 (-1=무제한) |
| `maxServices` | `Integer` | 최대 서비스 수 (-1=무제한) |
| `showAds` | `Boolean` | 광고 표시 여부 (FREE=true, BASIC=false) |
| `isTrialActive` | `Boolean` | 체험 기간 활성 여부 |
| `trialStartedAt` | `DateTime?` | 체험 시작일 |
| `trialEndsAt` | `DateTime?` | 체험 종료일 |
| `daysUntilTrialEnd` | `Long` | 체험 종료까지 남은 일수 (비활성이면 0) |
| `subscriptionStartedAt` | `DateTime?` | 유료 구독 시작일 (결제 완료 후 설정) |
| `nextBillingDate` | `DateTime?` | 다음 결제 예정일 |
| `currentStaffCount` | `Integer` | 현재 직원 수 |
| `currentMonthReservationCount` | `Integer` | 이번 달 예약 수 |
| `canUseService` | `Boolean` | 서비스 사용 가능 여부 |
| `canAddStaff` | `Boolean` | 직원 추가 가능 여부 |
| `canCreateReservation` | `Boolean` | 예약 생성 가능 여부 |

### isTrialActive / daysUntilTrialEnd 동작

```
체험 기간 중 (status=TRIAL, trialEndsAt 미래):
  isTrialActive = true
  daysUntilTrialEnd = 남은 일수 (예: 15)

체험 만료 후 (status=EXPIRED):
  isTrialActive = false
  daysUntilTrialEnd = 0 (또는 음수)

유료 결제 후 (status=ACTIVE):
  isTrialActive = false
  daysUntilTrialEnd = 0
```

---

## 2. Trial 관련 에러 코드 — 구현 완료

### 에러 코드 매핑

| 에러 코드 | HTTP 상태 | 의미 | 발생 시나리오 |
|-----------|-----------|------|--------------|
| `TR001` | 403 Forbidden | 체험판 만료 | FREE 플랜 + 체험 종료 후 유료 전용 API 호출 시 |
| `TR002` | 403 Forbidden | 체험판 기능 제한 | 체험 중(TRIAL)이지만 유료 전용 기능 접근 시 |
| `TR003` | 402 Payment Required | 업그레이드 필요 | FREE 플랜에서 유료 전용 기능 접근 시 (체험 아님) |

### 에러 응답 형식

```json
{
  "success": false,
  "error": {
    "code": "TR001",
    "message": "체험 기간이 종료되었습니다. 유료 플랜으로 업그레이드해 주세요."
  }
}
```

### 프론트엔드 분기 가이드

```typescript
// API 에러 핸들러
switch (error.code) {
  case 'TR001':
    // 체험 만료 모달 표시 → 결제 페이지 유도
    showTrialExpiredModal();
    break;
  case 'TR002':
    // 기능 잠금 모달 표시 (체험 중이지만 프리미엄 기능)
    showFeatureLockedModal('체험판에서는 사용할 수 없는 기능입니다.');
    break;
  case 'TR003':
    // 업그레이드 필요 모달 표시
    showUpgradeRequiredModal();
    break;
}
```

---

## 3. 체험 만료 후 자동 전환 스케줄러 — 구현 완료

### TrialExpirationScheduler

- **실행 시각**: 매일 새벽 2시 (cron: `0 0 2 * * *`)
- **KST 기준**: 서버 타임존이 Asia/Seoul이면 KST 02:00
- **동작**:
  1. `subscription_status = 'TRIAL'` AND `trial_ends_at < now()` 인 매장 조회
  2. 각 매장의 `subscription_status`를 `EXPIRED`로 변경
  3. `plan`은 변경하지 않음 (FREE 유지)

### 상태 전이

```
가입 시: FREE + TRIAL (30일)
         ↓ (30일 경과, 스케줄러 실행)
만료 후: FREE + EXPIRED
         ↓ (결제 완료)
결제 후: BASIC + ACTIVE
```

### 주요 포인트

- `plan`(FREE/BASIC)은 변경되지 않음. `status`만 TRIAL → EXPIRED
- EXPIRED 상태에서는 `canUseService = false` (서비스 사용 불가)
- 결제 완료 시 `activateSubscriptionAfterPayment()` 호출로 ACTIVE 전환

---

## 4. 월간 예약 카운트 리셋 — 구현 완료

### MonthlyResetScheduler

- **실행 시각**: 매월 1일 새벽 3시 (cron: `0 0 3 1 * *`)
- **KST 기준**: 서버 타임존 기준 매월 1일 03:00
- **동작**: 모든 매장의 `current_month_reservation_count`를 0으로 초기화
- **대상**: `current_month_reservation_count > 0`인 매장만 (효율적 쿼리)

### 프론트엔드 참고

- 리셋 후 `GET /api/subscription` 응답의 `currentMonthReservationCount`가 0으로 반환
- FREE 플랜(30건/월) 사용자는 매월 1일 예약 한도가 초기화됨

---

## 5. 유료 기능 목록 — 현재 적용 상태

### 현재 구현된 프리미엄 기능 (TR001/TR002/TR003 발생 대상)

| API | 에러 코드 | 상태 |
|-----|-----------|------|
| `GET /api/businesses/{id}/statistics/revenue` | TR001/TR002/TR003 | **구현 완료** |
| `GET /api/businesses/{id}/statistics/reservations` | TR001/TR002/TR003 | **구현 완료** |
| `GET /api/businesses/{id}/statistics/customers` | TR001/TR002/TR003 | **구현 완료** |
| `GET /api/businesses/{id}/statistics/staff` | TR001/TR002/TR003 | **구현 완료** |
| `GET /api/businesses/{id}/statistics/services` | TR001/TR002/TR003 | **구현 완료** |

### 미구현 프리미엄 기능 (Post-Beta)

아래 기능은 API 자체가 아직 미구현입니다. 구현 시 동일한 구독 체크가 자동 적용될 예정입니다.

| 기능 | API | 상태 |
|------|-----|------|
| 카카오톡 알림 | 미정 | **API 미구현** |
| 고객 태그 관리 | 미정 | **API 미구현** |
| 재방문 알림 | 미정 | **API 미구현** |
| 데이터 추출 (CSV/Excel) | 미정 | **API 미구현** |

### 사용량 제한 (별도 에러 코드)

이 에러들은 TR 코드가 아닌 별도 코드로 발생합니다:

| 제한 | 에러 코드 | FREE 기준 | BASIC 기준 |
|------|-----------|-----------|-----------|
| 직원 수 | `SL001` (STAFF_LIMIT_EXCEEDED) | 최대 1명 | 최대 5명 |
| 월간 예약 수 | `SL002` (RESERVATION_LIMIT_EXCEEDED) | 최대 30건/월 | 무제한 |
| 서비스 등록 수 | `SL004` (SERVICE_LIMIT_EXCEEDED) | 최대 10개 | 무제한 |

```json
// SL001 에러 응답 예시
{
  "success": false,
  "error": {
    "code": "SL001",
    "message": "직원 수 제한에 도달했습니다 (현재: 1명, 최대: 1명). 플랜을 업그레이드하세요."
  }
}
```

---

## 6. 구독 체크 로직 요약

### SubscriptionCheckService 동작 흐름

```
checkPremiumAccess(businessId) 호출 시:

1. BASIC 플랜 + ACTIVE 상태 → ✅ 통과
2. EXPIRED + 유료 결제 이력 없음 → ❌ TR001 (체험 만료)
3. TRIAL 상태 → ❌ TR002 (체험판 기능 제한)
4. FREE 플랜 → ❌ TR003 (업그레이드 필요)
5. 그 외 (취소/정지 등) → ❌ TR003 (업그레이드 필요)
```

### checkServiceAccess(businessId) — 기본 서비스 접근

```
1. TRIAL 또는 ACTIVE → ✅ 통과
2. EXPIRED (체험판 만료) → ❌ TR001
3. 그 외 (EXPIRED/CANCELED/SUSPENDED) → ❌ SU002
```

---

## 7. 추가 변경사항

### 서비스 등록 제한 (maxServices)

서비스 등록 API(`POST /api/businesses/{id}/services`)에 플랜별 제한이 적용되었습니다.

- FREE: 최대 10개
- BASIC: 무제한

초과 시 `SL004` (SERVICE_LIMIT_EXCEEDED) 에러가 반환됩니다.

### 프론트엔드 가이드

```typescript
// 서비스 등록 전 제한 체크 (프론트에서 사전 안내)
if (subscription.maxServices !== -1 && currentServiceCount >= subscription.maxServices) {
  showUpgradeModal('서비스 등록 한도에 도달했습니다.');
  return;
}
```
