# 가격정책 변경 결과 요약

> 작성일: 2026-02-19
> 4티어(FREE/BASIC/PRO/ENTERPRISE) → 2티어(FREE/BASIC) + 월/연간 결제 주기

---

## 1. DB 마이그레이션

### 실행 SQL (운영 DB에 수동 적용 필요)

```sql
-- 1) businesses 테이블에 billing_cycle 컬럼 추가
ALTER TABLE businesses ADD COLUMN IF NOT EXISTS billing_cycle VARCHAR(10) DEFAULT 'MONTHLY';

-- 2) payments 테이블에 billing_cycle 컬럼 추가
ALTER TABLE payments ADD COLUMN IF NOT EXISTS billing_cycle VARCHAR(10);

-- 3) 기존 PRO/ENTERPRISE 매장 → BASIC으로 전환
UPDATE businesses SET subscription_plan = 'BASIC', billing_cycle = 'MONTHLY'
WHERE subscription_plan IN ('PRO', 'ENTERPRISE');

-- 4) 기존 BASIC 매장 → billing_cycle 기본값 설정
UPDATE businesses SET billing_cycle = 'MONTHLY'
WHERE subscription_plan = 'BASIC' AND billing_cycle IS NULL;

-- 5) FREE 매장 → billing_cycle 기본값 설정
UPDATE businesses SET billing_cycle = 'MONTHLY'
WHERE billing_cycle IS NULL;
```

### 변경된 컬럼

| 테이블 | 컬럼 | 타입 | 기본값 | 설명 |
|--------|------|------|--------|------|
| `businesses` | `billing_cycle` | `VARCHAR(10)` | `'MONTHLY'` | 결제 주기 (MONTHLY/YEARLY) |
| `payments` | `billing_cycle` | `VARCHAR(10)` | NULL | 결제 시점의 결제 주기 |

---

## 2. API 변경사항 (프론트엔드 전달용)

### 2-1. 플랜 구조 변경

**Before (4티어)**
```
FREE → BASIC(29,000) → PRO(79,000) → ENTERPRISE(별도문의)
```

**After (2티어)**
```
FREE(무료) → BASIC(월 20,000 / 연 200,000)
```

| 플랜 | 월 가격 | 연 가격 | 직원 | 예약/월 | 서비스 | 광고 |
|------|---------|---------|------|---------|--------|------|
| FREE | 0 | 0 | 1명 | 30건 | 10개 | O |
| BASIC | 20,000원 | 200,000원 | 5명 | 무제한 | 무제한 | X |

> 프론트엔드에서 BASIC을 **"PAID(유료)"** 또는 **"유료"**로 표시 (어댑터 패턴)

### 2-2. `GET /api/subscription` 응답 변경

**추가된 필드:**

```json
{
  "plan": "BASIC",
  "status": "ACTIVE",
  "planDescription": "유료",
  "monthlyPrice": 20000,
  "yearlyPrice": 200000,        // NEW
  "billingCycle": "MONTHLY",    // NEW: "MONTHLY" | "YEARLY"
  "maxStaff": 5,
  "maxMonthlyReservations": -1,
  "maxServices": -1,            // NEW: -1 = 무제한
  "showAds": false,             // NEW: FREE이면 true
  "isTrialActive": false,
  "trialStartedAt": null,
  "trialEndsAt": null,
  "daysUntilTrialEnd": 0,
  "subscriptionStartedAt": "2026-02-01T00:00:00",
  "nextBillingDate": "2026-03-01T00:00:00",
  "currentStaffCount": 2,
  "currentMonthReservationCount": 15,
  "canUseService": true,
  "canAddStaff": true,
  "canCreateReservation": true
}
```

### 2-3. `POST /api/subscription/change-plan` 요청 변경

**Before:**
```json
{
  "newPlan": "BASIC"
}
```

**After:**
```json
{
  "newPlan": "BASIC",
  "billingCycle": "MONTHLY"   // NEW (선택): "MONTHLY" | "YEARLY"
}
```

### 2-4. `POST /api/payments` 요청 변경

**Before:**
```json
{
  "plan": "BASIC",
  "paymentMethod": "CARD",
  "couponCode": "WELCOME2026"
}
```

**After:**
```json
{
  "plan": "BASIC",
  "billingCycle": "MONTHLY",  // NEW (필수): "MONTHLY" | "YEARLY"
  "paymentMethod": "CARD",
  "couponCode": "WELCOME2026"
}
```

> 금액은 billingCycle에 따라 자동 계산됨:
> - `MONTHLY` → 20,000원
> - `YEARLY` → 200,000원

### 2-5. 결제 응답 (`PaymentResponse`) 변경

**추가된 필드:**
```json
{
  "id": 1,
  "subscriptionPlan": "BASIC",
  "billingCycle": "MONTHLY",   // NEW
  "billingPeriodStart": "2026-02-19",
  "billingPeriodEnd": "2026-03-19",
  "amount": 20000,
  "finalAmount": 20000,
  "paymentStatus": "COMPLETED"
}
```

---

## 3. 프론트엔드 어댑터 가이드

백엔드는 `FREE` / `BASIC` enum을 유지하되, 프론트엔드에서는 아래처럼 매핑:

```typescript
// 프론트엔드 어댑터 예시
const planDisplayMap = {
  FREE: { label: '무료', badge: 'FREE' },
  BASIC: { label: '유료', badge: 'PAID' },  // BASIC → "유료/PAID"로 표시
};

// 광고 표시 여부는 showAds 필드 사용
if (subscription.showAds) {
  // 광고 배너 표시
}

// 서비스 제한 체크
if (subscription.maxServices !== -1 && currentServiceCount >= subscription.maxServices) {
  // "서비스 등록 한도 초과" 안내 → 업그레이드 유도
}
```

---

## 4. 삭제된 항목

- `SubscriptionPlan.PRO` enum 값 제거
- `SubscriptionPlan.ENTERPRISE` enum 값 제거
- 프론트에서 PRO/ENTERPRISE 관련 UI 모두 제거 필요

---

## 5. 주의사항

1. **마이그레이션 순서**: DB 마이그레이션 먼저 → 백엔드 배포 → 프론트엔드 배포
2. **기존 PRO/ENTERPRISE 매장**: 자동으로 BASIC + MONTHLY로 전환됨
3. **연간 결제 기간**: `billingCycle=YEARLY` 시 billingPeriodEnd가 1년 후, nextBillingDate도 1년 후
4. **VAT**: 가격은 VAT 별도 (프론트에서 표시 시 "VAT 별도" 문구 필요)
