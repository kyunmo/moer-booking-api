# YEMO 프론트엔드 가격정책 변경 가이드

> 작성일: 2026-02-19
> 목적: 3티어(FREE/BASIC/PRO) → 2티어(FREE/PAID) + 월/연간 결제 전환
> 대상: 프론트엔드 수정 작업자용 참고 문서

---

## 1. 변경 전 → 후 비교

### 1.1 플랜 구조

| | 변경 전 | 변경 후 |
|---|---------|---------|
| 무료 | FREE (30건/월, 스태프 1명) | FREE (30건/월, 스태프 1명) |
| 유료 1 | BASIC (29,000원, 100건, 3명) | **PAID** (20,000원/월, 무제한, 5명) |
| 유료 2 | PRO (79,000원, 500건, 10명) | ❌ 삭제 |
| 유료 3 | ENTERPRISE (문의) | ❌ 삭제 |

### 1.2 결제 주기

| | 변경 전 | 변경 후 |
|---|---------|---------|
| 월 결제 | 29,000원/월 | **20,000원/월** (VAT 별도) |
| 연간 결제 | ❌ 없음 | **200,000원/년** (VAT 별도, 월 환산 16,667원) |

### 1.3 유료 플랜 기능 통합

변경 전 BASIC + PRO에 분산되어 있던 기능을 하나의 PAID 플랜으로 통합:

```
PAID 플랜 = 기존 BASIC 기능 + 기존 PRO 기능 일부

포함 기능:
- 예약 무제한
- 스태프 5명
- 시술 메뉴 무제한
- 예약 캘린더 (일/주/월 뷰)
- 예약 승인/거절
- 고객 자동 등록
- 방문 이력 무제한
- 고객 태그 관리
- 카카오톡 자동 알림
- 매출 통계 (오늘/주간/월간)
- 광고 제거
- 재방문 알림
- CSV/Excel 데이터 추출
```

---

## 2. 수정 대상 파일 목록

### 2.1 핵심 수정 (필수)

| 파일 | 변경 내용 | 우선순위 |
|------|-----------|---------|
| `src/components/pricing/PricingCard.vue` | 플랜 데이터 2티어로 변경, 월/연간 가격 표시 | ⭐⭐⭐ |
| `src/pages/pricing.vue` | 월/연 토글 추가, 3카드→2카드, FAQ 업데이트 | ⭐⭐⭐ |
| `src/components/public/landing/PricingPreviewSection.vue` | 3카드→2카드, 토글 추가 | ⭐⭐⭐ |
| `src/pages/register.vue` | 플랜 선택 3개→2개 | ⭐⭐⭐ |
| `src/pages/shop-admin/payment/index.vue` | 결제 주기 선택(월/연), 금액 계산 로직 | ⭐⭐⭐ |
| `src/components/subscription/PlanChangeDialog.vue` | 플랜 목록 2개로 변경 | ⭐⭐⭐ |

### 2.2 연관 수정 (후속)

| 파일 | 변경 내용 | 우선순위 |
|------|-----------|---------|
| `src/pages/faq.vue` | 요금 관련 FAQ 텍스트 업데이트 | ⭐⭐ |
| `src/pages/terms.vue` | 약관 내 요금 언급 부분 수정 | ⭐⭐ |
| `src/pages/refund-policy.vue` | 환불정책 금액 수정 | ⭐⭐ |
| 대시보드 내 업그레이드 유도 UI | CTA 문구 변경 | ⭐ |

---

## 3. 파일별 상세 변경 내용

### 3.1 `PricingCard.vue` — 핵심 변경

**현재:** plan prop으로 `'FREE' | 'BASIC' | 'PRO' | 'ENTERPRISE'` 받음, 컴포넌트 내부에 가격 하드코딩

**변경:**
- plan prop: `'FREE' | 'PAID'` 만 허용
- 새 prop 추가: `billingCycle` (`'monthly' | 'yearly'`)
- 가격 표시 로직 변경

```
변경 전 props:
  plan: 'FREE' | 'BASIC' | 'PRO' | 'ENTERPRISE'
  selected: Boolean
  compact: Boolean

변경 후 props:
  plan: 'FREE' | 'PAID'
  billingCycle: 'monthly' | 'yearly' (default: 'monthly')
  selected: Boolean
  compact: Boolean
```

**planInfo 데이터 변경:**

```js
// 변경 후
const planInfo = {
  FREE: {
    name: '무료',
    monthlyPrice: 0,
    yearlyPrice: 0,
    priceText: '0원',
    period: '',
    description: '가볍게 시작하기',
    color: 'info',
    icon: 'ri-gift-line',
    features: [
      '월 예약 30건',
      '스태프 1명',
      '시술 메뉴 10개',
      '기본 예약 관리',
      '고객 기본 정보',
      '방문 이력 (최근 10건)',
    ],
    limits: [
      '광고 표시',
      '카카오톡 알림 미지원',
      '고급 통계 미지원',
    ],
  },
  PAID: {
    name: '유료',
    monthlyPrice: 20000,
    yearlyPrice: 200000,
    monthlyPriceText: '20,000원',
    yearlyPriceText: '200,000원',
    yearlyMonthlyEquivalent: '16,667원',  // 200,000 / 12
    period: '/월',
    description: '모든 기능을 하나의 플랜으로',
    color: 'primary',
    icon: 'ri-vip-crown-line',
    badge: '2개월 무료',     // 연간 결제 시 표시
    features: [
      '예약 무제한',
      '스태프 5명',
      '시술 메뉴 무제한',
      '예약 캘린더 (일/주/월 뷰)',
      '예약 승인/거절',
      '고객 자동 등록 + 태그 관리',
      '방문 이력 무제한',
      '카카오톡 자동 알림',
      '매출 통계 (일/주/월)',
      '재방문 알림',
      'CSV/Excel 데이터 추출',
      '광고 제거',
    ],
  },
}
```

**가격 표시 로직:**

```
billingCycle === 'monthly' 일 때:
  → "20,000원 /월"
  → VAT 별도 표시

billingCycle === 'yearly' 일 때:
  → "16,667원 /월" (강조)
  → 아래에 "연 200,000원 (VAT 별도)" 표시
  → "2개월 무료" 배지 표시
  → 취소선으로 "20,000원" 원래 가격 표시
```

---

### 3.2 `pricing.vue` — 요금제 페이지

**주요 변경:**

1. **월/연간 토글 추가** (페이지 상단, 카드 위)
2. **3카드 → 2카드** 레이아웃 (md="6"으로 변경)
3. **상세 비교표** 2열로 변경
4. **FAQ** 업데이트

**토글 UI 구현:**

```
┌──────────────────────────────────────────┐
│                                          │
│     [ 월 결제 ]  ◉━━━━━●  [ 연간 결제 ] │
│                        ↑                 │
│                   "2개월 무료!"           │
│                                          │
└──────────────────────────────────────────┘
```

참고: `src/components/AppPricing.vue`에 이미 Materio 템플릿의 월/연간 토글 패턴이 있음. `annualMonthlyPlanPriceToggler` + `VSwitch` 구현 참고.

**state 변경:**

```js
// 변경 전
const selectedPlan = ref('BASIC')

// 변경 후
const billingCycle = ref('yearly')  // 기본값: 연간 (연간 결제를 기본으로 노출)
```

**plansDetail 변경:**

```js
// 변경 전: FREE, BASIC, PRO 3개
// 변경 후: FREE, PAID 2개만

const plansDetail = {
  FREE: { /* 위 3.1 참고 */ },
  PAID: { /* 위 3.1 참고 */ },
}
```

**VRow 레이아웃 변경:**

```html
<!-- 변경 전: 3카드 -->
<VRow justify="center">
  <VCol v-for="plan in ['FREE', 'BASIC', 'PRO']" cols="12" sm="6" md="4">

<!-- 변경 후: 2카드 -->
<VRow justify="center">
  <VCol v-for="plan in ['FREE', 'PAID']" cols="12" sm="6" md="5">
```

**상세 비교표 변경:**

```
변경 전 (4열): 기능 | 무료 | 베이직 | 프로
변경 후 (3열): 기능 | 무료 | 유료
```

**FAQ 수정:**

| FAQ | 변경 내용 |
|-----|-----------|
| VAT 별도? | 금액 수정: 20,000 + 2,000 = 22,000원/월 |
| 30일 무료 체험? | "유료 플랜을 30일간 무료 체험" (BASIC/PRO 구분 삭제) |
| 예약 초과? | "무료 플랜 30건 초과 시 → 유료 업그레이드 권장" (단순화) |
| 직원 수 초과? | "무료 1명 / 유료 5명" (단순화) |
| **신규 추가** | "연간 결제 시 할인이 있나요?" → "연간 결제 시 2개월 무료!" |
| **신규 추가** | "월 결제에서 연간 결제로 변경 가능?" → "네, 구독 관리에서 변경 가능" |

**route meta 수정:**

```yaml
meta:
  title: 요금제 - YEMO
  description: 무료로 시작하고 필요할 때 업그레이드. 월 20,000원으로 모든 기능을 사용하세요.
  keywords: 예약 시스템 가격, 요금제, 무료 체험, 월 결제, 연간 결제
```

---

### 3.3 `PricingPreviewSection.vue` — 랜딩 페이지 요금제 섹션

**변경:**

```html
<!-- 변경 전 -->
<VRow justify="center">
  <VCol v-for="plan in ['FREE', 'BASIC', 'PRO']" cols="12" sm="6" md="4">

<!-- 변경 후 -->
<!-- 1. 토글 추가 (간소화 버전) -->
<!-- 2. 2카드로 변경 -->
<VRow justify="center">
  <VCol v-for="plan in ['FREE', 'PAID']" cols="12" sm="6" md="5">
    <PricingCard
      :plan="plan"
      :billing-cycle="billingCycle"
      :selected="plan === 'PAID'"
      @select="handlePlanSelect"
    />
  </VCol>
</VRow>
```

랜딩 페이지에서는 토글을 넣되, 연간 결제가 기본 선택된 상태로 표시 (더 매력적인 가격이 먼저 보이도록).

---

### 3.4 `register.vue` — 회원가입 페이지

**변경:**

```html
<!-- 변경 전: 3개 플랜 선택 -->
<VCol v-for="plan in ['FREE', 'BASIC', 'PRO']" cols="12" md="4">
  <PricingCard :plan="plan" compact />
</VCol>

<!-- 변경 후: 2개 플랜 선택 + 결제 주기 -->
<VCol v-for="plan in ['FREE', 'PAID']" cols="12" md="6">
  <PricingCard :plan="plan" :billing-cycle="billingCycle" compact />
</VCol>

<!-- PAID 선택 시 결제 주기 선택 표시 -->
<VCol v-if="form.selectedPlan === 'PAID'" cols="12">
  <VBtnToggle v-model="billingCycle" mandatory color="primary">
    <VBtn value="monthly">월 결제 (20,000원/월)</VBtn>
    <VBtn value="yearly">연간 결제 (200,000원/년) — 2개월 무료!</VBtn>
  </VBtnToggle>
</VCol>
```

**form 데이터 변경:**

```js
// 변경 전
const form = ref({
  selectedPlan: route.query.plan || 'BASIC',
  // ...
})

// 변경 후
const form = ref({
  selectedPlan: route.query.plan || 'PAID',
  billingCycle: 'yearly',  // 기본: 연간
  // ...
})
```

**체험 안내 문구 변경:**

```
변경 전: "베이직 플랜을 30일간 무료로 체험"
변경 후: "유료 플랜의 모든 기능을 30일간 무료로 체험"
```

---

### 3.5 `payment/index.vue` — 구독/결제 페이지

**주요 변경:**

1. **플랜 선택 영역:** FREE / PAID 2개만 표시
2. **결제 주기 선택 추가:** 월/연간 라디오 또는 토글
3. **금액 계산 로직 변경**

**결제 주기 선택 UI:**

```html
<!-- 플랜 선택 아래에 결제 주기 선택 추가 -->
<VRadioGroup v-model="billingCycle" v-if="selectedPlan === 'PAID'">
  <VRadio value="monthly">
    <template #label>
      <div>
        <span class="font-weight-bold">월 결제</span>
        <span class="text-body-2 ms-2">20,000원/월 (VAT 별도)</span>
      </div>
    </template>
  </VRadio>
  <VRadio value="yearly">
    <template #label>
      <div>
        <span class="font-weight-bold">연간 결제</span>
        <span class="text-body-2 ms-2">200,000원/년 (VAT 별도)</span>
        <VChip color="success" size="x-small" class="ms-2">2개월 무료</VChip>
      </div>
    </template>
  </VRadio>
</VRadioGroup>
```

**금액 계산 변경:**

```js
// 변경 전
const originalAmount = computed(() => {
  if (selectedPlan.value === 'BASIC') return 29000
  if (selectedPlan.value === 'PRO') return 79000
  return 0
})

// 변경 후
const originalAmount = computed(() => {
  if (selectedPlan.value !== 'PAID') return 0
  return billingCycle.value === 'yearly' ? 200000 : 20000
})

const vatAmount = computed(() => Math.round(originalAmount.value * 0.1))
const totalAmount = computed(() => originalAmount.value + vatAmount.value)

// 할인 표시 (연간 결제 시)
const savedAmount = computed(() => {
  if (billingCycle.value === 'yearly') {
    return 20000 * 12 - 200000  // 40,000원 절약
  }
  return 0
})
```

**결제 금액 표시:**

```
월 결제 선택 시:
  상품 금액: 20,000원
  VAT (10%): 2,000원
  합계: 22,000원

연간 결제 선택 시:
  상품 금액: 200,000원 (월 20,000원 × 10개월)
  절약 금액: -40,000원 (2개월 무료!)
  VAT (10%): 20,000원
  합계: 220,000원
```

---

### 3.6 `PlanChangeDialog.vue` — 플랜 변경 다이얼로그

**변경:**

```js
// 변경 전: 4개 플랜
const plans = [
  { value: 'FREE', name: '무료', price: '0원' },
  { value: 'BASIC', name: '베이직', price: '29,000원' },
  { value: 'PRO', name: '프로', price: '79,000원' },
  { value: 'ENTERPRISE', name: '엔터프라이즈', price: '문의' },
]

// 변경 후: 2개 플랜 + 결제 주기
const plans = [
  { value: 'FREE', name: '무료', price: '0원' },
  { value: 'PAID', name: '유료', monthlyPrice: '20,000원/월', yearlyPrice: '200,000원/년' },
]
```

다운그레이드 (PAID → FREE) 시 주의 안내:
- "현재 결제 주기 종료 시점에 무료 플랜으로 전환됩니다"
- "무료 플랜 제한 (30건/월, 스태프 1명) 적용"

업그레이드 (FREE → PAID) 시:
- 결제 주기 선택 UI 표시 (월/연간)
- "30일 무료 체험" 안내

---

## 4. 공통 상수 파일 제안

여러 컴포넌트에서 플랜 정보가 중복되므로 상수 파일로 추출 권장:

**`src/constants/pricing.js` (신규 생성)**

```js
export const BILLING_CYCLES = {
  MONTHLY: 'monthly',
  YEARLY: 'yearly',
}

export const PLANS = {
  FREE: {
    key: 'FREE',
    name: '무료',
    monthlyPrice: 0,
    yearlyPrice: 0,
    maxReservations: 30,    // 월 제한
    maxStaff: 1,
    maxServices: 10,
    features: [
      '월 예약 30건',
      '스태프 1명',
      '시술 메뉴 10개',
      '기본 예약 관리',
      '고객 기본 정보',
      '방문 이력 (최근 10건)',
    ],
    excludedFeatures: [
      '카카오톡 알림',
      '고객 태그 관리',
      '매출 통계',
      '재방문 알림',
      '데이터 추출',
    ],
  },
  PAID: {
    key: 'PAID',
    name: '유료',
    monthlyPrice: 20000,
    yearlyPrice: 200000,
    maxReservations: -1,    // 무제한
    maxStaff: 5,
    maxServices: -1,        // 무제한
    features: [
      '예약 무제한',
      '스태프 5명',
      '시술 메뉴 무제한',
      '예약 캘린더 (일/주/월 뷰)',
      '예약 승인/거절',
      '고객 자동 등록 + 태그 관리',
      '방문 이력 무제한',
      '카카오톡 자동 알림',
      '매출 통계 (일/주/월)',
      '재방문 알림',
      'CSV/Excel 데이터 추출',
      '광고 제거',
    ],
    excludedFeatures: [],
  },
}

// 헬퍼 함수
export function getPlanPrice(planKey, billingCycle) {
  const plan = PLANS[planKey]
  if (!plan) return 0
  return billingCycle === BILLING_CYCLES.YEARLY
    ? plan.yearlyPrice
    : plan.monthlyPrice
}

export function getMonthlyEquivalent(planKey, billingCycle) {
  const plan = PLANS[planKey]
  if (!plan) return 0
  return billingCycle === BILLING_CYCLES.YEARLY
    ? Math.round(plan.yearlyPrice / 12)
    : plan.monthlyPrice
}

export function getYearlySavings(planKey) {
  const plan = PLANS[planKey]
  if (!plan) return 0
  return (plan.monthlyPrice * 12) - plan.yearlyPrice
}

export function formatCurrency(amount) {
  return new Intl.NumberFormat('ko-KR').format(amount) + '원'
}
```

---

## 5. 백엔드 enum 매핑 참고

현재 백엔드 `SubscriptionPlan` enum은 별도 문서에서 변경 요청 예정.
프론트에서 먼저 작업할 때 아래 매핑을 임시로 사용:

```
프론트 'FREE'  ↔ 백엔드 SubscriptionPlan.FREE
프론트 'PAID'  ↔ 백엔드 SubscriptionPlan.BASIC (임시, 추후 PAID로 변경)
```

백엔드 변경 전까지는 API 호출 시 `'BASIC'`으로 전송하고,
프론트 UI에만 `'PAID'`로 표시하는 어댑터 패턴 사용:

```js
// src/utils/planAdapter.js
export function toBackend(frontPlan) {
  return frontPlan === 'PAID' ? 'BASIC' : frontPlan
}

export function toFrontend(backendPlan) {
  return ['BASIC', 'PRO'].includes(backendPlan) ? 'PAID' : backendPlan
}
```

---

## 6. 체크리스트

### Phase 1: 핵심 UI 변경

- [ ] `src/constants/pricing.js` 생성
- [ ] `PricingCard.vue` 수정 (2티어 + billingCycle prop)
- [ ] `pricing.vue` 수정 (토글 + 2카드 + FAQ)
- [ ] `PricingPreviewSection.vue` 수정 (랜딩 페이지)
- [ ] `register.vue` 수정 (플랜 선택 2개 + 결제 주기)

### Phase 2: 결제/구독 연동

- [ ] `payment/index.vue` 수정 (결제 주기 선택 + 금액 계산)
- [ ] `PlanChangeDialog.vue` 수정 (플랜 변경)
- [ ] `planAdapter.js` 생성 (백엔드 호환용 임시)

### Phase 3: 텍스트/문서 업데이트

- [ ] `faq.vue` 요금 관련 FAQ 수정
- [ ] `terms.vue` 약관 내 요금 관련 수정
- [ ] `refund-policy.vue` 금액 수정
- [ ] route meta description/keywords 수정

### Phase 4: 정리

- [ ] PRO, ENTERPRISE 관련 코드 완전 삭제
- [ ] `AppPricing.vue` (Materio 원본) — 사용 안 하면 삭제
- [ ] 전체 grep: `29,000`, `79,000`, `'PRO'`, `'ENTERPRISE'` 잔여 확인
