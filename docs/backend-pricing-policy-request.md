# 가격정책 변경 - 백엔드 요청사항 (v2)

> **작성일:** 2026-02-19 (v2 업데이트)
> **변경 사항:** 4티어(FREE/BASIC/PRO/ENTERPRISE) → 2티어(FREE/PAID) + 월/연간 결제 주기
> **프론트엔드:** 반영 완료
> **이전 버전 대비 변경:** BASIC → PAID 용어 변경, 월/연간 결제 주기 추가, VAT 별도 기준 정립

---

## 1. 변경 요약

### AS-IS (기존 4티어)

| 플랜 | 월 가격 | 월 예약 | 스태프 수 | 비고 |
|------|---------|---------|-----------|------|
| FREE | 0원 | 30건 | 1명 | 광고 표시 |
| BASIC | 29,000원 (VAT 별도) | 100건 | 3명 | |
| PRO | 79,000원 (VAT 별도) | 500건 | 10명 | |
| ENTERPRISE | 문의 | 무제한 | 무제한 | |

### TO-BE (신규 2티어 + 결제 주기)

| 플랜 | 결제 주기 | 가격 (VAT 별도) | VAT 포함 | 월 예약 | 스태프 수 | 비고 |
|------|-----------|-----------------|----------|---------|-----------|------|
| FREE | - | 0원 | 0원 | 30건 | 1명 | 광고 표시, 기능 제한 |
| PAID | **월간** | **20,000원/월** | 22,000원/월 | **무제한** | **5명** | 전체 기능 |
| PAID | **연간** | **200,000원/년** | 220,000원/년 | **무제한** | **5명** | 2개월 무료 (월 환산 16,667원) |

### 연간 결제 할인 상세

```
월간 결제: 20,000원 × 12개월 = 240,000원/년 (VAT 별도)
연간 결제: 200,000원/년 (VAT 별도)
절약 금액: 40,000원/년 (VAT 별도) = 2개월분 무료
할인율: 약 16.7%
```

---

## 2. 용어 정의

| 용어 | 설명 |
|------|------|
| FREE | 무료 플랜 |
| PAID | 유료 플랜 (프론트엔드 표시명) |
| billingCycle | 결제 주기: `monthly` (월간) 또는 `yearly` (연간) |
| VAT 별도 | 모든 가격은 VAT(10%) 별도 기준. 결제 시 VAT 별도 계산 |

> **중요:** 프론트엔드에서는 `PAID`로 표시하지만, 백엔드 Plan enum은 당분간 `BASIC`을 유지해도 됩니다. 프론트엔드에서 어댑터 패턴(`planAdapter.js`)으로 변환합니다.
> - 프론트 `PAID` ↔ 백엔드 `BASIC` (임시 매핑)
> - 추후 백엔드에서도 `PAID`로 변경 시 어댑터 제거 예정

---

## 3. 백엔드 수정 필요 사항

### 3.1 Subscription 엔티티/테이블 변경

```
현재 plan 컬럼 허용값: FREE, BASIC, PRO, ENTERPRISE
변경 plan 컬럼 허용값: FREE, BASIC (추후 PAID로 변경 가능)
```

**신규 컬럼 추가 필요:**

```sql
ALTER TABLE subscription ADD COLUMN billing_cycle VARCHAR(10) DEFAULT 'monthly';
-- 허용값: 'monthly', 'yearly'
```

**주의:** 기존 PRO/ENTERPRISE 사용자가 있을 수 있으므로 마이그레이션 전략 필요
- 기존 PRO → BASIC(PAID)으로 자동 전환
- 기존 ENTERPRISE → BASIC(PAID)으로 전환 또는 별도 협의
- 기존 BASIC → billing_cycle='monthly'로 설정

### 3.2 플랜별 제한값 변경

```java
// AS-IS
FREE:       maxStaff=1,  maxMonthlyReservations=30
BASIC:      maxStaff=3,  maxMonthlyReservations=100
PRO:        maxStaff=10, maxMonthlyReservations=500
ENTERPRISE: maxStaff=-1, maxMonthlyReservations=-1

// TO-BE
FREE:  maxStaff=1, maxMonthlyReservations=30, maxServices=10
BASIC: maxStaff=5, maxMonthlyReservations=-1, maxServices=-1  // 무제한
```

> **신규:** `maxServices` (시술 메뉴 수) 필드 추가 여부 확인 필요

### 3.3 가격 변경 (VAT 별도 기준)

```java
// AS-IS
BASIC: 29,000원/월 (VAT 별도)
PRO:   79,000원/월 (VAT 별도)

// TO-BE
BASIC(PAID) monthly: 20,000원/월 (VAT 별도)
BASIC(PAID) yearly:  200,000원/년 (VAT 별도)
```

가격 결정 로직:

```java
public int getPrice(String plan, String billingCycle) {
    if ("FREE".equals(plan)) return 0;
    if ("yearly".equals(billingCycle)) return 200000;
    return 20000; // monthly 기본
}
```

### 3.4 구독 API 응답 변경 (`GET /api/subscription`)

프론트엔드에서 사용하는 필드 목록 (**신규 필드 포함**):

```json
{
  "plan": "FREE | BASIC",
  "planDescription": "무료 | 유료",
  "billingCycle": "monthly | yearly",
  "monthlyPrice": 0 | 20000,
  "yearlyPrice": 0 | 200000,
  "status": "ACTIVE | TRIAL | EXPIRED | CANCELED | SUSPENDED",
  "maxStaff": 1 | 5,
  "maxMonthlyReservations": 30 | -1,
  "maxServices": 10 | -1,
  "currentStaffCount": 0,
  "currentMonthReservationCount": 0,
  "canAddStaff": true | false,
  "canCreateReservation": true | false,
  "canUseService": true | false,
  "isTrialActive": true | false,
  "daysUntilTrialEnd": 30,
  "nextBillingDate": "2026-03-19",
  "trialEndDate": "2026-03-19",
  "showAds": true | false
}
```

**신규 필드:**
- `billingCycle`: 현재 결제 주기 (`monthly` | `yearly`)
- `yearlyPrice`: 연간 가격 (VAT 별도)
- `maxServices`: 시술 메뉴 제한 수
- `showAds`: 광고 표시 여부 (FREE=true, PAID=false)

### 3.5 플랜 변경 API (`POST /api/subscription/change-plan`)

```json
{
  "plan": "BASIC",
  "billingCycle": "monthly | yearly"
}
```

- 허용 플랜: FREE, BASIC (PRO, ENTERPRISE 제거)
- **신규:** `billingCycle` 필드 추가
- FREE → BASIC 변경 시: billingCycle 필수
- BASIC → FREE 변경 시: billingCycle 무시

**결제 주기 변경 시나리오:**
1. `monthly → yearly`: 남은 월 구독 일수 일할 계산 → 연간 가격에서 차감
2. `yearly → monthly`: 남은 연간 구독 일수 일할 계산 → 환불 처리
3. `FREE → BASIC(monthly/yearly)`: 30일 무료 체험 후 결제 시작

### 3.6 결제 API (`POST /api/payments`)

```json
{
  "plan": "BASIC",
  "billingCycle": "monthly | yearly",
  "paymentMethod": "CARD | BANK_TRANSFER | VIRTUAL_ACCOUNT | MOBILE",
  "couponCode": "optional"
}
```

**신규:** `billingCycle` 필드 추가

**결제 금액 계산 (서버 측):**

```java
int basePrice;
if ("yearly".equals(billingCycle)) {
    basePrice = 200000;  // 연간
} else {
    basePrice = 20000;   // 월간
}

int vatAmount = Math.round(basePrice * 0.1);  // VAT 10%
int totalAmount = basePrice + vatAmount;        // 최종 결제 금액

// 쿠폰 할인 적용
if (couponCode != null) {
    int discount = calculateCouponDiscount(couponCode, basePrice);
    totalAmount = totalAmount - discount;
}
```

**결제 금액 예시:**

| 결제 주기 | 상품 금액 | VAT (10%) | 합계 |
|-----------|-----------|-----------|------|
| 월간 | 20,000원 | 2,000원 | **22,000원** |
| 연간 | 200,000원 | 20,000원 | **220,000원** |

---

## 4. 광고 정책

### AS-IS
- 무료 플랜: 광고 표시
- 유료 플랜: 광고 제거

### TO-BE
- **무료 플랜: 광고 표시** (유료 전환 유도)
- **유료 플랜: 광고 제거**

**백엔드 조치:**
- `showAds` 필드를 구독 API 응답에 포함
- FREE 플랜: `showAds = true`
- BASIC(PAID) 플랜: `showAds = false`

---

## 5. 기능 제한 정책 (FREE vs PAID)

| 기능 | FREE | PAID |
|------|------|------|
| 월 예약 건수 | 30건 | 무제한 |
| 스태프 수 | 1명 | 5명 |
| 시술 메뉴 | 10개 | 무제한 |
| 예약 캘린더 | 기본 | 일/주/월 뷰 |
| 예약 승인/거절 | X | O |
| 고객 자동 등록 | X | O |
| 방문 이력 | 최근 10건 | 무제한 |
| 고객 태그 관리 | X | O |
| 카카오톡 자동 알림 | X | O |
| 통계 및 리포트 | X | O |
| 디자이너별 성과 | X | O |
| 재방문 알림 | X | O |
| CSV/Excel 데이터 추출 | X | O |
| 광고 | 표시 | 제거 |

**질문:** 현재 백엔드에서 위 기능 제한이 `plan` 기준으로 분기 처리되어 있나요?
아니면 `maxStaff`, `maxMonthlyReservations` 외에 별도 기능 플래그가 필요한가요?

**제안:** 기능 플래그 방식

```json
{
  "features": {
    "calendarViews": ["day"] | ["day", "week", "month"],
    "approveReject": false | true,
    "autoRegisterCustomer": false | true,
    "visitHistoryLimit": 10 | -1,
    "customerTags": false | true,
    "kakaoNotification": false | true,
    "statistics": false | true,
    "staffPerformance": false | true,
    "revisitReminder": false | true,
    "dataExport": false | true,
    "showAds": true | false
  }
}
```

또는 단순히 `plan` 값으로 프론트엔드에서 분기 처리 (현재 방식).

---

## 6. 체험판(Trial) 정책

| 항목 | 값 |
|------|-----|
| 체험 기간 | 30일 |
| 체험 플랜 | BASIC/PAID (전체 기능) |
| 체험 결제 주기 | 미적용 (체험 기간이므로) |
| 체험 종료 후 | 무료 플랜으로 자동 전환 |
| 신용카드 등록 | 불필요 |
| 자동 과금 | 없음 |

**확인 필요:**
- `trialEndDate` 이후 자동으로 FREE로 전환되는 로직이 있는지?
- `resetMonthlyReservationCounts()` 스케줄러 구현 상태?
- 체험판에서 유료 전환 시 `billingCycle` 선택 화면 필요

---

## 7. 결제 주기 전환 로직

### 7.1 월간 → 연간 전환

```
시나리오: 월간 결제 중 연간으로 변경
1. 현재 월간 구독 남은 일수 계산
2. 남은 일수분 일할 금액 환산 (크레딧)
3. 연간 가격 - 크레딧 = 추가 결제 금액
4. 추가 결제 완료 → 연간 구독 시작 (현재일 기준 1년)

예시:
- 월간 20,000원, 15일 남음
- 크레딧: 20,000 × (15/30) = 10,000원
- 연간 결제: 200,000 - 10,000 = 190,000원 추가 결제
- VAT 별도로 계산
```

### 7.2 연간 → 월간 전환

```
시나리오: 연간 결제 중 월간으로 변경
1. 연간 구독 사용 기간 계산
2. 사용 기간분 월간 가격 기준 환산
3. 연간 결제 금액 - 사용 금액 = 환불 금액
4. 환불 처리 → 다음 결제일부터 월간 구독

예시:
- 연간 200,000원, 3개월 사용
- 사용 금액: 20,000 × 3 = 60,000원 (월간 가격 기준)
- 환불: 200,000 - 60,000 = 140,000원
- VAT 별도로 계산
```

### 7.3 자동 갱신

```
월간 구독: 매월 결제일에 20,000원 + VAT 자동 결제
연간 구독: 매년 결제일에 200,000원 + VAT 자동 결제
```

---

## 8. DB 마이그레이션

### 8.1 테이블 스키마 변경

```sql
-- billing_cycle 컬럼 추가
ALTER TABLE subscription ADD COLUMN billing_cycle VARCHAR(10) DEFAULT 'monthly';

-- yearly_price 컬럼 추가 (선택적)
ALTER TABLE subscription ADD COLUMN yearly_price INT DEFAULT 0;
```

### 8.2 기존 데이터 처리

```sql
-- PRO/ENTERPRISE 사용자를 BASIC으로 전환
UPDATE subscription
SET plan = 'BASIC',
    max_staff = 5,
    max_monthly_reservations = -1,
    monthly_price = 20000,
    yearly_price = 200000,
    billing_cycle = 'monthly'
WHERE plan IN ('PRO', 'ENTERPRISE');

-- 기존 BASIC 사용자 가격/제한 업데이트
UPDATE subscription
SET max_staff = 5,
    max_monthly_reservations = -1,
    monthly_price = 20000,
    yearly_price = 200000
WHERE plan = 'BASIC';

-- FREE 사용자 yearly_price 설정
UPDATE subscription
SET yearly_price = 0
WHERE plan = 'FREE';
```

### 8.3 Enum/상수 정리

```java
// AS-IS
public enum Plan { FREE, BASIC, PRO, ENTERPRISE }

// TO-BE (1단계: 최소 변경)
public enum Plan { FREE, BASIC }

// TO-BE (2단계: 명칭 변경, 선택적)
public enum Plan { FREE, PAID }

// 결제 주기 Enum (신규)
public enum BillingCycle { MONTHLY, YEARLY }
```

---

## 9. 프론트엔드 변경 완료 파일 목록

| 파일 | 변경 내용 |
|------|----------|
| `src/constants/pricing.js` | **(신규)** 중앙 가격 상수, 헬퍼 함수 |
| `src/utils/planAdapter.js` | **(신규)** 프론트↔백엔드 플랜명 변환 |
| `src/components/pricing/PricingCard.vue` | FREE/PAID 2티어, billingCycle prop, 월/연간 가격 표시 |
| `src/pages/pricing.vue` | 월/연간 토글, PAID 플랜, FAQ 업데이트 |
| `src/pages/index.vue` | 메타 설명 가격 변경 (20,000원) |
| `src/components/public/landing/HeroSection.vue` | "월 20,000원부터" |
| `src/components/public/landing/PricingPreviewSection.vue` | 2카드 + 월/연간 토글 |
| `src/pages/register.vue` | 2플랜 + 결제 주기 선택 |
| `src/pages/shop-admin/subscription/index.vue` | PAID 플랜 비교 |
| `src/components/subscription/PlanChangeDialog.vue` | PAID 플랜, planLimits 변경 |
| `src/pages/shop-admin/payment/index.vue` | 결제 주기 선택 + VAT 별도 계산 |
| `src/pages/features.vue` | BASIC 뱃지 → 유료 |
| `src/pages/faq.vue` | 전체 FAQ 유료 플랜 반영, 연간 결제 FAQ 추가 |

---

## 10. 확인 요청 체크리스트

### 필수 (1단계)

- [ ] `Plan` enum에서 PRO, ENTERPRISE 제거
- [ ] BASIC(PAID) 플랜 제한값 변경 (maxStaff=5, maxMonthlyReservations=-1)
- [ ] 가격 변경 (월간: 20,000원, 연간: 200,000원, VAT 별도)
- [ ] `billing_cycle` 컬럼 추가 (`monthly` | `yearly`)
- [ ] 구독 API 응답에 `billingCycle`, `yearlyPrice` 필드 추가
- [ ] 결제 API 요청에 `billingCycle` 필드 추가
- [ ] 플랜 변경 API에 `billingCycle` 필드 추가

### 필수 (2단계)

- [ ] 연간 결제 자동 갱신 로직
- [ ] 월간 ↔ 연간 결제 주기 전환 로직 (일할 계산)
- [ ] 체험판 종료 후 FREE 자동 전환 로직 확인
- [ ] 월간 예약 카운트 초기화 스케줄러 구현 여부
- [ ] 기존 PRO/ENTERPRISE 사용자 마이그레이션 실행

### 선택적

- [ ] `Plan` enum을 `BASIC` → `PAID`로 명칭 변경 (프론트 어댑터 제거 가능)
- [ ] `showAds` 필드 추가 (FREE=true, PAID=false)
- [ ] `maxServices` 필드 추가 (FREE=10, PAID=-1)
- [ ] FREE 플랜 기능 제한 (통계, 태그, 알림 등) 백엔드 분기 처리 방법 결정
- [ ] 기능 플래그 API 방식 vs plan 기반 프론트 분기 방식 선택

---

## 11. API 변경 요약 (Quick Reference)

### GET /api/subscription — 응답 필드 추가

```diff
  {
    "plan": "FREE | BASIC",
-   "planDescription": "무료 | 베이직",
+   "planDescription": "무료 | 유료",
+   "billingCycle": "monthly | yearly",
    "monthlyPrice": 0 | 20000,
+   "yearlyPrice": 0 | 200000,
    "status": "ACTIVE | TRIAL | EXPIRED | CANCELED | SUSPENDED",
    "maxStaff": 1 | 5,
    "maxMonthlyReservations": 30 | -1,
+   "maxServices": 10 | -1,
+   "showAds": true | false,
    ...기존 필드 유지
  }
```

### POST /api/subscription/change-plan — 요청 필드 추가

```diff
  {
    "plan": "BASIC",
+   "billingCycle": "monthly | yearly"
  }
```

### POST /api/payments — 요청 필드 추가

```diff
  {
    "plan": "BASIC",
+   "billingCycle": "monthly | yearly",
    "paymentMethod": "CARD | BANK_TRANSFER | VIRTUAL_ACCOUNT | MOBILE",
    "couponCode": "optional"
  }
```

---

> **참고:** 가격 정책의 근거는 `docs/가격정책.md` 참조
> **프론트엔드 가이드:** `docs/YEMO-프론트엔드-가격정책-변경-가이드.md` 참조
> **프론트엔드 작업자:** Claude Code
> **최종 수정일:** 2026-02-19
