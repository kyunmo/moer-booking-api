# moer SaaS 서비스 최종 계획 요약

> **작성일**: 2026-02-11  
> **수정일**: 2026-02-11 (계획 간소화)

---

## 📋 핵심 변경사항

### ✅ 삭제된 페이지 (초기 출시에서 제외)
1. ~~업종별 페이지~~ (`/industries/:type`)
   - 미용실, 필라테스, 스터디카페 별도 페이지
   - → 랜딩 페이지에 간단히 언급만

2. ~~고객 사례~~ (`/case-studies`, `/case-studies/:id`)
   - 성공 사례 목록/상세 페이지
   - → 실제 고객 확보 후 추가 예정

3. ~~블로그~~ (`/blog`)
   - 선택 사항
   - → 초기에는 불필요

### ✅ 무료 체험 기간 변경
- **기존**: 7일 무료 체험
- **변경**: **30일 무료 체험**
- **이유**: 기존 설계와 일치, 충분한 체험 기간 제공

---

## 🎯 최종 페이지 구조

### 공개 영역 (6개 페이지)
1. **홈** (`/`) - 랜딩 페이지
2. **기능 소개** (`/features`)
3. **요금제** (`/pricing`)
4. **FAQ** (`/faq`)
5. **로그인** (`/login`)
6. **회원가입** (`/signup`)

### 인증 영역 (로그인 후)
1. **대시보드** (`/dashboard`) - 기존 예약 시스템
2. **구독 관리** (`/subscription`)
3. **결제** (`/subscription/checkout`)
4. **계정 설정** (`/account`)

### 슈퍼 관리자 영역
1. **관리자 대시보드** (`/admin`)
2. **매장 관리** (`/admin/businesses`)
3. **사용자 관리** (`/admin/users`)
4. **구독 관리** (`/admin/subscriptions`)
5. **결제 내역** (`/admin/payments`)
6. **쿠폰 관리** (`/admin/coupons`)
7. **감사 로그** (`/admin/audit-logs`)

---

## 📝 작성 완료된 컨텐츠

### ✅ 사용할 컨텐츠 (5개)
1. ✅ `landing-page-content.md` - 랜딩 페이지
2. ✅ `features-page-content.md` - 기능 소개
3. ✅ `pricing-page-content.md` - 요금제
4. ✅ `faq-page-content.md` - FAQ
5. ✅ `login-signup-content.md` - 로그인/회원가입

### ⚠️ 보류된 컨텐츠 (나중에 사용)
- `industry-beauty-salon-content.md`
- `industry-pilates-content.md`
- `industry-study-cafe-content.md`
- `case-studies-list-content.md`
- `case-study-detail-content.md`

---

## 🎨 프론트엔드 디렉토리 구조 (간소화)

```
src/
├─ views/
│  ├─ public/              # 공개 페이지 (6개)
│  │  ├─ HomePage.vue
│  │  ├─ FeaturesPage.vue
│  │  ├─ PricingPage.vue
│  │  ├─ FAQPage.vue
│  │  ├─ LoginPage.vue
│  │  └─ SignupPage.vue
│  │
│  ├─ subscription/        # 구독 관리 (4개)
│  │  ├─ SubscriptionPage.vue
│  │  ├─ CheckoutPage.vue
│  │  ├─ PaymentSuccessPage.vue
│  │  └─ PaymentCancelPage.vue
│  │
│  ├─ dashboard/           # 기존 예약 시스템 (유지)
│  │  └─ ...
│  │
│  └─ admin/               # 슈퍼 관리자 (기존 + 확장)
│     ├─ AdminDashboardPage.vue (기존)
│     ├─ BusinessesPage.vue (기존)
│     ├─ UsersPage.vue (기존)
│     ├─ AuditLogsPage.vue (기존)
│     ├─ SubscriptionsPage.vue (🆕 추가)
│     ├─ PaymentsPage.vue (🆕 추가)
│     └─ CouponsPage.vue (🆕 추가)
│
├─ layouts/
│  ├─ PublicLayout.vue     # 공개 페이지 레이아웃
│  ├─ AppLayout.vue        # 앱 레이아웃 (기존)
│  └─ AdminLayout.vue      # 관리자 레이아웃 (기존 + 확장)
│
├─ components/
│  ├─ public/              # 공개 페이지용 컴포넌트
│  │  ├─ PublicHeader.vue
│  │  ├─ PublicFooter.vue
│  │  ├─ HeroSection.vue
│  │  ├─ PricingCard.vue
│  │  └─ FAQAccordion.vue
│  │
│  ├─ subscription/        # 구독 관리용
│  │  ├─ PlanSelector.vue
│  │  ├─ PaymentForm.vue
│  │  └─ UsageIndicator.vue
│  │
│  └─ common/              # 공통 컴포넌트
│
└─ stores/
   ├─ auth.js (기존 확장)
   ├─ subscription.js (🆕 추가)
   └─ payment.js (🆕 추가)
```

---

## 🗄️ 데이터베이스 확장

### 기존 테이블 수정

#### businesses 테이블에 구독 컬럼 추가

```sql
ALTER TABLE businesses
ADD COLUMN subscription_plan VARCHAR(20) DEFAULT 'FREE', 
    -- 'FREE', 'BASIC', 'PRO', 'ENTERPRISE'
ADD COLUMN subscription_status VARCHAR(20) DEFAULT 'TRIAL',
    -- 'TRIAL', 'ACTIVE', 'EXPIRED', 'CANCELED', 'SUSPENDED'
ADD COLUMN trial_ends_at TIMESTAMP,
ADD COLUMN trial_started_at TIMESTAMP,
ADD COLUMN subscription_started_at TIMESTAMP,
ADD COLUMN next_billing_date TIMESTAMP;
```

### 새로 추가할 테이블

1. **payments** (결제 내역)
2. **coupons** (쿠폰)
3. **coupon_usages** (쿠폰 사용 내역)

---

## 🚀 구현 우선순위

### Phase 1 (1-2주) - 랜딩 및 기본 구조 ⭐ 최우선
- [x] 컨텐츠 작성 완료
- [ ] PublicLayout.vue
- [ ] HomePage.vue (랜딩)
- [ ] FeaturesPage.vue
- [ ] PricingPage.vue
- [ ] FAQPage.vue
- [ ] LoginPage.vue
- [ ] SignupPage.vue

### Phase 2 (1-2주) - 구독 관리
- [ ] businesses 테이블 컬럼 추가
- [ ] SubscriptionPage.vue
- [ ] 플랜 변경 기능
- [ ] 무료 체험 자동 설정 (가입 시 30일)

### Phase 3 (1주) - Fake 결제
- [ ] CheckoutPage.vue
- [ ] FakePGService 구현
- [ ] 결제 성공/실패 페이지

### Phase 4 (1주) - 슈퍼 관리자 확장
- [ ] SubscriptionsPage.vue (관리자)
- [ ] PaymentsPage.vue (관리자)
- [ ] CouponsPage.vue (관리자)

### Phase 5 (나중에) - 실제 PG 연동
- [ ] 토스페이먼츠 계정
- [ ] TossPaymentsService
- [ ] 결제 위젯 통합

---

## 💡 핵심 포인트

### 1. 간소화된 구조
- **초기 출시**: 핵심 기능만 (6개 공개 페이지)
- **점진적 확장**: 고객 사례, 업종별 페이지는 나중에

### 2. 30일 무료 체험
- 가입 시 자동으로 `trial_ends_at = 가입일 + 30일` 설정
- 체험 기간 동안 선택한 플랜(베이직/프로) 모든 기능 사용
- 체험 종료 후 자동 과금 없음

### 3. 기존 구조 최대한 활용
- businesses 테이블에 구독 컬럼만 추가
- 기존 User, Business 엔티티 확장
- 슈퍼 관리자 기능 확장 (중복 없이)

### 4. 단계적 결제 연동
- **Phase 3**: FakePGService로 전체 프로세스 구현
- **Phase 5**: 실제 토스페이먼츠 연동

---

## 📌 다음 단계

### Option A: 백엔드부터
```sql
1. businesses 테이블 컬럼 추가
2. payments, coupons 테이블 생성
3. SubscriptionService 구현
4. FakePGService 구현
```

### Option B: 프론트엔드부터
```
1. PublicLayout.vue 생성
2. HomePage.vue 개발 (랜딩 페이지)
3. PricingPage.vue
4. SignupPage.vue (플랜 선택 포함)
```

### Option C: 동시 진행
```
1명 개발자이므로 순차 진행 권장
백엔드 API → 프론트엔드 UI 순서 추천
```

---

**문서 작성일**: 2026-02-11  
**최종 수정**: 2026-02-11  
**상태**: 계획 간소화 완료 ✅
