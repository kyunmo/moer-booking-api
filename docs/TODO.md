# moer SaaS 서비스 구현 TODO

> **생성일**: 2026-02-12
> **기반 문서**: `/docs/moer-final-plan-summary.md`
> **진행 상황**: `/docs/reports/progress-2026-02-12.md`

---

## 📊 전체 진행률

```
전체 진행률: ████░░░░░░░░░░░░░░░░ 20% (백엔드 기반 완성)
```

**Phase별 진행률**:
- Phase 1 (랜딩 페이지): 0% (컨텐츠만 작성 완료)
- Phase 2 (구독 관리): 0%
- Phase 3 (Fake 결제): 0%
- Phase 4 (슈퍼 관리자 확장): 0%
- Phase 5 (실제 PG 연동): 0%

---

## 🎯 Phase 1: 랜딩 페이지 및 기본 구조 (1-2주) ⭐ 최우선

### 백엔드 작업

#### 1.1 회원가입 플랜 선택 기능 추가
- [ ] `SignupRequest` DTO 확장
  - [ ] `selectedPlan` 필드 추가 (FREE, BASIC, PRO, ENTERPRISE)
  - [ ] Validation 추가
- [ ] `AuthService.register()` 수정
  - [ ] 회원가입 시 선택한 플랜 저장
  - [ ] 무료 체험 자동 설정 (`trial_ends_at = 가입일 + 30일`)

**파일**:
- `src/main/java/io/moer/booking/domain/auth/dto/SignupRequest.java`
- `src/main/java/io/moer/booking/domain/auth/service/AuthService.java`

**예상 소요**: 2시간

---

### 프론트엔드 작업

#### 1.2 공개 페이지 레이아웃
- [ ] `PublicLayout.vue` 생성
  - [ ] PublicHeader.vue (로고, 네비게이션, 로그인 버튼)
  - [ ] PublicFooter.vue (회사 정보, 링크)
  - [ ] 반응형 디자인 (모바일/태블릿/데스크톱)

**파일**:
- `frontend/src/layouts/PublicLayout.vue`
- `frontend/src/components/public/PublicHeader.vue`
- `frontend/src/components/public/PublicFooter.vue`

**예상 소요**: 6시간

---

#### 1.3 홈 페이지 (랜딩)
- [ ] `HomePage.vue` 개발
  - [ ] Hero 섹션 (메인 캐치프레이즈, CTA 버튼)
  - [ ] 주요 기능 소개 (3-4개 섹션)
  - [ ] 요금제 미리보기
  - [ ] FAQ 미리보기
  - [ ] CTA 섹션 (무료 체험 시작)

**컨텐츠 소스**: `/docs/landing-page-content.md`

**파일**:
- `frontend/src/views/public/HomePage.vue`
- `frontend/src/components/public/HeroSection.vue`

**예상 소요**: 12시간

---

#### 1.4 기능 소개 페이지
- [ ] `FeaturesPage.vue` 개발
  - [ ] 전체 기능 목록 (11개 도메인)
  - [ ] 각 기능별 상세 설명 + 스크린샷
  - [ ] 업종별 활용 예시

**컨텐츠 소스**: `/docs/features-page-content.md`

**파일**:
- `frontend/src/views/public/FeaturesPage.vue`

**예상 소요**: 8시간

---

#### 1.5 요금제 페이지
- [ ] `PricingPage.vue` 개발
  - [ ] 요금제 비교 테이블 (FREE, BASIC, PRO, ENTERPRISE)
  - [ ] 기능 비교 체크리스트
  - [ ] 30일 무료 체험 강조
  - [ ] 플랜 선택 버튼 (회원가입 페이지로 이동)

**컨텐츠 소스**: `/docs/pricing-page-content.md`

**컴포넌트**:
- [ ] `PricingCard.vue` (요금제 카드)
- [ ] `PricingComparison.vue` (비교 테이블)

**파일**:
- `frontend/src/views/public/PricingPage.vue`
- `frontend/src/components/public/PricingCard.vue`

**예상 소요**: 10시간

---

#### 1.6 FAQ 페이지
- [ ] `FAQPage.vue` 개발
  - [ ] FAQ 아코디언 (카테고리별)
  - [ ] 검색 기능
  - [ ] 도움말 링크

**컨텐츠 소스**: `/docs/faq-page-content.md`

**컴포넌트**:
- [ ] `FAQAccordion.vue` (아코디언)
- [ ] `FAQSearch.vue` (검색)

**파일**:
- `frontend/src/views/public/FAQPage.vue`
- `frontend/src/components/public/FAQAccordion.vue`

**예상 소요**: 6시간

---

#### 1.7 로그인 페이지
- [ ] `LoginPage.vue` 수정 (기존 확장)
  - [ ] 디자인 개선 (공개 레이아웃)
  - [ ] OAuth2 버튼 (Google, Kakao, Naver)
  - [ ] "무료로 시작하기" 버튼 추가

**컨텐츠 소스**: `/docs/login-signup-content.md`

**파일**:
- `frontend/src/views/public/LoginPage.vue`

**예상 소요**: 4시간

---

#### 1.8 회원가입 페이지
- [ ] `SignupPage.vue` 수정 (기존 확장)
  - [ ] 플랜 선택 단계 추가
    - Step 1: 플랜 선택 (FREE, BASIC, PRO, ENTERPRISE)
    - Step 2: 계정 정보 입력
    - Step 3: 매장 정보 입력 (선택)
  - [ ] 30일 무료 체험 안내
  - [ ] OAuth2 회원가입

**컨텐츠 소스**: `/docs/login-signup-content.md`

**컴포넌트**:
- [ ] `PlanSelector.vue` (플랜 선택)
- [ ] `SignupStepIndicator.vue` (단계 표시)

**파일**:
- `frontend/src/views/public/SignupPage.vue`
- `frontend/src/components/public/PlanSelector.vue`

**예상 소요**: 10시간

---

#### 1.9 라우터 설정
- [ ] `router/index.js` 수정
  - [ ] 공개 라우트 추가 (`/`, `/features`, `/pricing`, `/faq`, `/login`, `/signup`)
  - [ ] PublicLayout 적용
  - [ ] 메타 정보 (SEO)

**파일**:
- `frontend/src/router/index.js`

**예상 소요**: 2시간

---

### Phase 1 완료 조건
- [x] 컨텐츠 작성 완료
- [ ] 공개 페이지 6개 완성
- [ ] 회원가입 시 플랜 선택 가능
- [ ] 30일 무료 체험 자동 설정

**Phase 1 총 예상 소요**: 60시간 (약 1.5주)

---

## 🔄 Phase 2: 구독 관리 (1-2주)

### 백엔드 작업

#### 2.1 데이터베이스 확장
- [ ] `businesses` 테이블 컬럼 추가
  ```sql
  ALTER TABLE businesses
  ADD COLUMN subscription_plan VARCHAR(20) DEFAULT 'FREE',
  ADD COLUMN subscription_status VARCHAR(20) DEFAULT 'TRIAL',
  ADD COLUMN trial_ends_at TIMESTAMP,
  ADD COLUMN trial_started_at TIMESTAMP,
  ADD COLUMN subscription_started_at TIMESTAMP,
  ADD COLUMN next_billing_date TIMESTAMP;
  ```
- [ ] Migration 스크립트 작성
- [ ] 초기 데이터 마이그레이션 (기존 매장 → FREE 플랜)

**파일**:
- `src/main/resources/db/migration/V2__add_subscription_to_businesses.sql`

**예상 소요**: 2시간

---

#### 2.2 Business Entity 확장
- [ ] `Business.java` 수정
  - [ ] `subscriptionPlan` 필드 추가 (Enum)
  - [ ] `subscriptionStatus` 필드 추가 (Enum)
  - [ ] `trialEndsAt`, `trialStartedAt` 필드 추가
  - [ ] `subscriptionStartedAt`, `nextBillingDate` 필드 추가
  - [ ] 헬퍼 메서드
    - `isTrialActive()` - 체험판 활성 여부
    - `isTrialExpired()` - 체험판 만료 여부
    - `getDaysUntilTrialEnd()` - 체험판 남은 일수
    - `canUseFeature(String feature)` - 기능 사용 가능 여부

**파일**:
- `src/main/java/io/moer/booking/domain/business/Business.java`

**예상 소요**: 2시간

---

#### 2.3 Subscription Enum 생성
- [ ] `SubscriptionPlan.java` Enum 생성
  - FREE, BASIC, PRO, ENTERPRISE
  - 각 플랜별 제한 (최대 직원 수, 월간 예약 수)
- [ ] `SubscriptionStatus.java` Enum 생성
  - TRIAL, ACTIVE, EXPIRED, CANCELED, SUSPENDED

**파일**:
- `src/main/java/io/moer/booking/domain/business/SubscriptionPlan.java`
- `src/main/java/io/moer/booking/domain/business/SubscriptionStatus.java`

**예상 소요**: 1시간

---

#### 2.4 DTO 확장
- [ ] `BusinessResponse.java` 수정
  - [ ] 구독 정보 필드 추가
- [ ] `SubscriptionInfoResponse.java` 생성
  - [ ] 현재 플랜, 상태, 체험판 정보, 사용량 통계

**파일**:
- `src/main/java/io/moer/booking/domain/business/dto/BusinessResponse.java`
- `src/main/java/io/moer/booking/domain/subscription/dto/SubscriptionInfoResponse.java`

**예상 소요**: 2시간

---

#### 2.5 SubscriptionService 생성
- [ ] `SubscriptionService.java` 생성
  - [ ] `getSubscriptionInfo(businessId)` - 구독 정보 조회
  - [ ] `changePlan(businessId, newPlan)` - 플랜 변경
  - [ ] `cancelSubscription(businessId)` - 구독 취소
  - [ ] `checkTrialStatus(businessId)` - 체험판 상태 체크
  - [ ] `getUsageStats(businessId)` - 사용량 통계

**파일**:
- `src/main/java/io/moer/booking/domain/subscription/service/SubscriptionService.java`

**예상 소요**: 6시간

---

#### 2.6 SubscriptionController 생성
- [ ] `SubscriptionController.java` 생성
  - [ ] `GET /api/subscription` - 구독 정보 조회
  - [ ] `POST /api/subscription/change-plan` - 플랜 변경
  - [ ] `POST /api/subscription/cancel` - 구독 취소
  - [ ] `GET /api/subscription/usage` - 사용량 조회

**파일**:
- `src/main/java/io/moer/booking/domain/subscription/controller/SubscriptionController.java`

**예상 소요**: 4시간

---

#### 2.7 사용량 제한 로직 추가
- [ ] `UsageLimitService.java` 생성
  - [ ] `checkStaffLimit(businessId)` - 직원 수 제한 체크
  - [ ] `checkReservationLimit(businessId)` - 월간 예약 수 제한 체크
  - [ ] `canAddStaff(businessId)` - 직원 추가 가능 여부
  - [ ] `canCreateReservation(businessId)` - 예약 생성 가능 여부

**파일**:
- `src/main/java/io/moer/booking/domain/subscription/service/UsageLimitService.java`

**예상 소요**: 4시간

---

#### 2.8 기존 Service 수정 (제한 로직 적용)
- [ ] `StaffService.java` 수정
  - [ ] `createStaff()` 시 직원 수 제한 체크
- [ ] `ReservationService.java` 수정
  - [ ] `createReservation()` 시 월간 예약 수 제한 체크

**파일**:
- `src/main/java/io/moer/booking/domain/staff/service/StaffService.java`
- `src/main/java/io/moer/booking/domain/reservation/service/ReservationService.java`

**예상 소요**: 3시간

---

### 프론트엔드 작업

#### 2.9 Subscription Store 생성
- [ ] `stores/subscription.js` 생성
  - [ ] State: subscriptionInfo, usageStats
  - [ ] Actions: fetchSubscriptionInfo, changePlan, cancelSubscription
  - [ ] Getters: isTrialActive, daysUntilTrialEnd, canUseFeature

**파일**:
- `frontend/src/stores/subscription.js`

**예상 소요**: 3시간

---

#### 2.10 구독 관리 페이지
- [ ] `SubscriptionPage.vue` 생성
  - [ ] 현재 플랜 정보 표시
  - [ ] 체험판 진행률 표시
  - [ ] 사용량 통계 (직원 수, 월간 예약 수)
  - [ ] 플랜 변경 버튼
  - [ ] 구독 취소 버튼

**컴포넌트**:
- [ ] `UsageIndicator.vue` (사용량 표시)
- [ ] `TrialProgressBar.vue` (체험판 진행률)
- [ ] `PlanUpgradeButton.vue` (플랜 업그레이드)

**파일**:
- `frontend/src/views/subscription/SubscriptionPage.vue`
- `frontend/src/components/subscription/UsageIndicator.vue`

**예상 소요**: 10시간

---

#### 2.11 플랜 변경 다이얼로그
- [ ] `PlanChangeDialog.vue` 생성
  - [ ] 플랜 선택 (BASIC, PRO, ENTERPRISE)
  - [ ] 플랜 비교 (현재 vs 새 플랜)
  - [ ] 예상 금액 표시
  - [ ] 변경 확인

**파일**:
- `frontend/src/components/subscription/PlanChangeDialog.vue`

**예상 소요**: 6시간

---

#### 2.12 대시보드 확장 (체험판 알림)
- [ ] `DashboardPage.vue` 수정
  - [ ] 체험판 만료 임박 알림 (7일 이하)
  - [ ] 사용량 초과 경고
  - [ ] 플랜 업그레이드 권장

**파일**:
- `frontend/src/views/dashboard/DashboardPage.vue`

**예상 소요**: 4시간

---

### Phase 2 완료 조건
- [ ] businesses 테이블 확장 완료
- [ ] 구독 관리 API 구현
- [ ] 사용량 제한 로직 적용
- [ ] 구독 관리 페이지 완성
- [ ] 플랜 변경 기능 작동

**Phase 2 총 예상 소요**: 47시간 (약 1.2주)

---

## 💳 Phase 3: Fake 결제 (1주)

### 백엔드 작업

#### 3.1 Payment 테이블 생성
- [ ] `payments` 테이블 생성
  ```sql
  CREATE TABLE payments (
      id BIGSERIAL PRIMARY KEY,
      business_id BIGINT NOT NULL REFERENCES businesses(id),
      amount INTEGER NOT NULL,
      payment_method VARCHAR(20), -- CARD, TRANSFER
      payment_status VARCHAR(20), -- PENDING, COMPLETED, FAILED, REFUNDED
      pg_transaction_id VARCHAR(100),
      pg_provider VARCHAR(20), -- FAKE, TOSS
      paid_at TIMESTAMP,
      created_at TIMESTAMP DEFAULT NOW()
  );
  ```

**파일**:
- `src/main/resources/db/migration/V3__create_payments_table.sql`

**예상 소요**: 1시간

---

#### 3.2 Payment Entity & DTO 생성
- [ ] `Payment.java` Entity 생성
- [ ] `PaymentStatus.java` Enum 생성
- [ ] `PaymentResponse.java` DTO 생성
- [ ] `PaymentCreateRequest.java` DTO 생성

**파일**:
- `src/main/java/io/moer/booking/domain/payment/Payment.java`
- `src/main/java/io/moer/booking/domain/payment/PaymentStatus.java`
- `src/main/java/io/moer/booking/domain/payment/dto/`

**예상 소요**: 2시간

---

#### 3.3 PaymentRepository & Mapper 생성
- [ ] `PaymentRepository.java` 생성
- [ ] `PaymentMapper.xml` 생성
  - [ ] INSERT, SELECT, UPDATE 쿼리

**파일**:
- `src/main/java/io/moer/booking/domain/payment/repository/PaymentRepository.java`
- `src/main/resources/mapper/PaymentMapper.xml`

**예상 소요**: 2시간

---

#### 3.4 FakePGService 생성
- [ ] `FakePGService.java` 생성
  - [ ] `requestPayment(amount, plan)` - 결제 요청 (항상 성공)
  - [ ] `verifyPayment(transactionId)` - 결제 검증
  - [ ] `refundPayment(transactionId)` - 환불 처리
  - [ ] 랜덤 Transaction ID 생성
  - [ ] 2초 지연 (실제 PG 시뮬레이션)

**파일**:
- `src/main/java/io/moer/booking/domain/payment/service/FakePGService.java`

**예상 소요**: 3시간

---

#### 3.5 PaymentService 생성
- [ ] `PaymentService.java` 생성
  - [ ] `createPayment(businessId, plan)` - 결제 생성
  - [ ] `processPayment(paymentId)` - 결제 처리 (FakePG 호출)
  - [ ] `completePayment(paymentId)` - 결제 완료 (구독 활성화)
  - [ ] `failPayment(paymentId)` - 결제 실패
  - [ ] `refundPayment(paymentId)` - 환불

**파일**:
- `src/main/java/io/moer/booking/domain/payment/service/PaymentService.java`

**예상 소요**: 5시간

---

#### 3.6 PaymentController 생성
- [ ] `PaymentController.java` 생성
  - [ ] `POST /api/payment/create` - 결제 생성
  - [ ] `POST /api/payment/process` - 결제 처리
  - [ ] `GET /api/payment/{id}` - 결제 조회
  - [ ] `GET /api/payment/history` - 결제 내역

**파일**:
- `src/main/java/io/moer/booking/domain/payment/controller/PaymentController.java`

**예상 소요**: 3시간

---

#### 3.7 SubscriptionService 확장 (결제 연동)
- [ ] `activateSubscription(businessId, paymentId)` - 구독 활성화
  - [ ] 체험판 종료
  - [ ] 구독 시작 (`subscription_status = ACTIVE`)
  - [ ] 다음 결제일 설정

**파일**:
- `src/main/java/io/moer/booking/domain/subscription/service/SubscriptionService.java`

**예상 소요**: 2시간

---

### 프론트엔드 작업

#### 3.8 Payment Store 생성
- [ ] `stores/payment.js` 생성
  - [ ] State: currentPayment, paymentHistory
  - [ ] Actions: createPayment, processPayment, fetchHistory

**파일**:
- `frontend/src/stores/payment.js`

**예상 소요**: 2시간

---

#### 3.9 결제 페이지
- [ ] `CheckoutPage.vue` 생성
  - [ ] 선택한 플랜 정보 표시
  - [ ] 결제 금액 표시
  - [ ] 결제 수단 선택 (카드만)
  - [ ] "결제하기" 버튼 (FakePG 호출)
  - [ ] 로딩 스피너 (2초)

**파일**:
- `frontend/src/views/subscription/CheckoutPage.vue`

**예상 소요**: 8시간

---

#### 3.10 결제 성공 페이지
- [ ] `PaymentSuccessPage.vue` 생성
  - [ ] 성공 메시지
  - [ ] 구독 정보 표시
  - [ ] 대시보드로 이동 버튼

**파일**:
- `frontend/src/views/subscription/PaymentSuccessPage.vue`

**예상 소요**: 3시간

---

#### 3.11 결제 실패 페이지
- [ ] `PaymentCancelPage.vue` 생성
  - [ ] 실패 메시지
  - [ ] 재시도 버튼
  - [ ] 고객센터 연락 정보

**파일**:
- `frontend/src/views/subscription/PaymentCancelPage.vue`

**예상 소요**: 2시간

---

### Phase 3 완료 조건
- [ ] payments 테이블 생성 완료
- [ ] FakePG 서비스 구현
- [ ] 결제 프로세스 완성 (생성 → 처리 → 완료)
- [ ] 결제 페이지 3개 완성
- [ ] 결제 성공 시 구독 활성화

**Phase 3 총 예상 소요**: 33시간 (약 1주)

---

## 🔐 Phase 4: 슈퍼 관리자 확장 (1주)

### 백엔드 작업

#### 4.1 Coupon 테이블 생성
- [ ] `coupons` 테이블 생성
  ```sql
  CREATE TABLE coupons (
      id BIGSERIAL PRIMARY KEY,
      code VARCHAR(50) UNIQUE NOT NULL,
      discount_type VARCHAR(20), -- PERCENT, FIXED
      discount_value INTEGER NOT NULL,
      max_uses INTEGER,
      used_count INTEGER DEFAULT 0,
      valid_from TIMESTAMP,
      valid_until TIMESTAMP,
      created_at TIMESTAMP DEFAULT NOW()
  );
  ```
- [ ] `coupon_usages` 테이블 생성
  ```sql
  CREATE TABLE coupon_usages (
      id BIGSERIAL PRIMARY KEY,
      coupon_id BIGINT REFERENCES coupons(id),
      business_id BIGINT REFERENCES businesses(id),
      payment_id BIGINT REFERENCES payments(id),
      used_at TIMESTAMP DEFAULT NOW()
  );
  ```

**파일**:
- `src/main/resources/db/migration/V4__create_coupon_tables.sql`

**예상 소요**: 1시간

---

#### 4.2 Coupon Entity & DTO 생성
- [ ] `Coupon.java` Entity 생성
- [ ] `CouponUsage.java` Entity 생성
- [ ] `CouponResponse.java` DTO 생성
- [ ] `CouponCreateRequest.java` DTO 생성

**파일**:
- `src/main/java/io/moer/booking/domain/coupon/`

**예상 소요**: 2시간

---

#### 4.3 CouponService 생성
- [ ] `CouponService.java` 생성
  - [ ] `createCoupon(request)` - 쿠폰 생성
  - [ ] `validateCoupon(code)` - 쿠폰 검증
  - [ ] `applyCoupon(code, businessId)` - 쿠폰 적용
  - [ ] `listCoupons()` - 쿠폰 목록
  - [ ] `deleteCoupon(id)` - 쿠폰 삭제

**파일**:
- `src/main/java/io/moer/booking/domain/coupon/service/CouponService.java`

**예상 소요**: 4시간

---

#### 4.4 SuperAdminController 확장
- [ ] `SuperAdminCouponController.java` 생성
  - [ ] `POST /api/admin/coupons` - 쿠폰 생성
  - [ ] `GET /api/admin/coupons` - 쿠폰 목록
  - [ ] `DELETE /api/admin/coupons/{id}` - 쿠폰 삭제
  - [ ] `GET /api/admin/coupons/{id}/usages` - 사용 내역

**파일**:
- `src/main/java/io/moer/booking/domain/superadmin/controller/SuperAdminCouponController.java`

**예상 소요**: 3시간

---

#### 4.5 SuperAdmin 구독 관리 API
- [ ] `SuperAdminSubscriptionController.java` 생성
  - [ ] `GET /api/admin/subscriptions` - 전체 구독 현황
  - [ ] `POST /api/admin/subscriptions/{id}/extend-trial` - 체험판 연장
  - [ ] `POST /api/admin/subscriptions/{id}/force-activate` - 강제 활성화
  - [ ] `POST /api/admin/subscriptions/{id}/suspend` - 정지

**파일**:
- `src/main/java/io/moer/booking/domain/superadmin/controller/SuperAdminSubscriptionController.java`

**예상 소요**: 4시간

---

#### 4.6 SuperAdmin 결제 관리 API
- [ ] `SuperAdminPaymentController.java` 생성
  - [ ] `GET /api/admin/payments` - 전체 결제 내역
  - [ ] `GET /api/admin/payments/stats` - 결제 통계
  - [ ] `POST /api/admin/payments/{id}/refund` - 환불 처리

**파일**:
- `src/main/java/io/moer/booking/domain/superadmin/controller/SuperAdminPaymentController.java`

**예상 소요**: 3시간

---

### 프론트엔드 작업

#### 4.7 관리자 구독 관리 페이지
- [ ] `admin/SubscriptionsPage.vue` 생성
  - [ ] 전체 구독 목록 (검색, 필터)
  - [ ] 구독 상태별 통계
  - [ ] 체험판 연장 버튼
  - [ ] 강제 활성화/정지 버튼

**파일**:
- `frontend/src/views/admin/SubscriptionsPage.vue`

**예상 소요**: 8시간

---

#### 4.8 관리자 결제 관리 페이지
- [ ] `admin/PaymentsPage.vue` 생성
  - [ ] 전체 결제 내역 (검색, 필터)
  - [ ] 결제 통계 (일별, 월별)
  - [ ] 환불 처리 버튼
  - [ ] 엑셀 다운로드

**파일**:
- `frontend/src/views/admin/PaymentsPage.vue`

**예상 소요**: 8시간

---

#### 4.9 관리자 쿠폰 관리 페이지
- [ ] `admin/CouponsPage.vue` 생성
  - [ ] 쿠폰 목록
  - [ ] 쿠폰 생성 다이얼로그
  - [ ] 쿠폰 사용 내역 조회
  - [ ] 쿠폰 삭제

**파일**:
- `frontend/src/views/admin/CouponsPage.vue`

**예상 소요**: 8시간

---

#### 4.10 관리자 대시보드 확장
- [ ] `admin/AdminDashboardPage.vue` 수정 (기존)
  - [ ] 구독 통계 추가
  - [ ] 결제 통계 추가
  - [ ] 수익 차트

**파일**:
- `frontend/src/views/admin/AdminDashboardPage.vue`

**예상 소요**: 4시간

---

### Phase 4 완료 조건
- [ ] 쿠폰 시스템 구현 완료
- [ ] 관리자 구독/결제/쿠폰 관리 페이지 완성
- [ ] 관리자 대시보드에 통계 추가

**Phase 4 총 예상 소요**: 45시간 (약 1.1주)

---

## 🏦 Phase 5: 실제 PG 연동 (나중에)

### 백엔드 작업

#### 5.1 토스페이먼츠 계정 생성
- [ ] 토스페이먼츠 계정 생성
- [ ] Client Key, Secret Key 발급
- [ ] Webhook URL 설정

**예상 소요**: 2시간

---

#### 5.2 TossPaymentsService 생성
- [ ] `TossPaymentsService.java` 생성
  - [ ] `requestPayment(amount, orderId)` - 결제 요청
  - [ ] `verifyPayment(paymentKey, orderId, amount)` - 결제 검증
  - [ ] `cancelPayment(paymentKey, cancelReason)` - 결제 취소
  - [ ] Webhook 처리

**파일**:
- `src/main/java/io/moer/booking/domain/payment/service/TossPaymentsService.java`

**예상 소요**: 8시간

---

#### 5.3 PaymentService 수정 (TossPG 연동)
- [ ] FakePG → TossPG 전환 로직
- [ ] `application.yml`에 `payment.provider` 설정

**파일**:
- `src/main/java/io/moer/booking/domain/payment/service/PaymentService.java`
- `src/main/resources/application.yml`

**예상 소요**: 4시간

---

#### 5.4 Webhook Controller 생성
- [ ] `PaymentWebhookController.java` 생성
  - [ ] `POST /api/payment/webhook/toss` - 토스페이먼츠 Webhook

**파일**:
- `src/main/java/io/moer/booking/domain/payment/controller/PaymentWebhookController.java`

**예상 소요**: 4시간

---

### 프론트엔드 작업

#### 5.5 토스페이먼츠 결제 위젯 통합
- [ ] `CheckoutPage.vue` 수정
  - [ ] 토스페이먼츠 SDK 로드
  - [ ] 결제 위젯 렌더링
  - [ ] 결제 성공/실패 콜백 처리

**파일**:
- `frontend/src/views/subscription/CheckoutPage.vue`

**예상 소요**: 6시간

---

### Phase 5 완료 조건
- [ ] 토스페이먼츠 계정 생성 완료
- [ ] 실제 PG 연동 완료
- [ ] Webhook 처리 완료
- [ ] 프론트엔드 결제 위젯 통합

**Phase 5 총 예상 소요**: 24시간 (약 3일)

---

## 📊 전체 작업 요약

| Phase | 항목 | 예상 소요 | 우선순위 |
|-------|------|----------|---------|
| **Phase 1** | 랜딩 페이지 및 기본 구조 | 60시간 (1.5주) | ⭐⭐⭐⭐⭐ |
| **Phase 2** | 구독 관리 | 47시간 (1.2주) | ⭐⭐⭐⭐ |
| **Phase 3** | Fake 결제 | 33시간 (1주) | ⭐⭐⭐⭐ |
| **Phase 4** | 슈퍼 관리자 확장 | 45시간 (1.1주) | ⭐⭐⭐ |
| **Phase 5** | 실제 PG 연동 | 24시간 (3일) | ⭐⭐ |
| **합계** | | **209시간** (약 5.2주) | |

---

## 🎯 다음 액션

### 즉시 시작 가능한 작업 (Phase 1)

1. **백엔드**: 회원가입 플랜 선택 기능 추가 (2시간)
   - `SignupRequest.java` 확장
   - `AuthService.java` 수정

2. **프론트엔드**: PublicLayout.vue 생성 (6시간)
   - Header, Footer 컴포넌트

3. **프론트엔드**: HomePage.vue 개발 (12시간)
   - 랜딩 페이지 구현

**권장 순서**:
```
1. 백엔드 회원가입 플랜 선택 (2시간)
   ↓
2. 프론트엔드 PublicLayout (6시간)
   ↓
3. 프론트엔드 HomePage (12시간)
   ↓
4. 프론트엔드 PricingPage (10시간)
   ↓
5. 프론트엔드 SignupPage 확장 (10시간)
```

---

## 📝 작업 관리

- [ ] TODO 문서 작성 완료 ✅
- [ ] Phase 1 시작
- [ ] Phase 2 시작
- [ ] Phase 3 시작
- [ ] Phase 4 시작
- [ ] Phase 5 시작

---

**문서 버전**: 1.0
**최종 수정**: 2026-02-12
**작성자**: Claude Code
**상태**: 작업 준비 완료 ✅
