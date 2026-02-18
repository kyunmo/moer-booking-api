# 프론트엔드 TODO 리스트

> **작성일:** 2026-02-18
> **기준 문서:** `docs/YEMO-종합분석-TODO-2026-02-18.md`
> **목적:** 프론트엔드 기준 작업 목록 + 각 단계별 백엔드 요청사항 정리
> **원칙:** 화면에서 검증하면서 필요한 API를 백엔드에 요청하는 방식

---

## 사전 분석: 종합분석 대비 실제 코드 상태 차이

종합분석 문서와 실제 코드를 대조한 결과, 일부 항목은 이미 완료된 상태:

| 종합분석 항목 | 실제 상태 | 비고 |
|-------------|----------|------|
| F-1: alert() → useSnackbar 통일 | **이미 완료** | 프로젝트 코드에 alert() 없음 |
| F-6: NavBar 알림 더미 데이터 제거 | **이미 완료** | 실제 API 기반 폴링 구현됨 |
| F-9: ReservationDialog 레거시 정리 | **이미 완료** | 파일 없음 (ReservationFormDialog로 대체됨) |
| D항: 관리자 프로필 페이지 "미구현" | **다이얼로그로 구현됨** | ProfileDialog.vue (4탭: 기본정보/비밀번호/SNS/탈퇴) |

---

## Phase A: 출시 차단 이슈 해결

> 베타 출시 전 반드시 완료해야 할 프론트엔드 작업

### A-1. 약관/개인정보처리방침 정식 버전 교체

**현황:** `terms.vue`, `privacy.vue`에 임시 텍스트 존재. 하단에 "임시 버전" 경고 표시 중.

**작업:**
- [ ] 정식 이용약관 내용 작성 및 교체 (`src/pages/terms.vue`)
    - B2B2C 특성 반영: 플랫폼↔매장, 매장↔고객 관계 구분
    - 서비스 이용 범위, 책임 한계, 계정 관리, 분쟁 해결
- [ ] 정식 개인정보처리방침 작성 및 교체 (`src/pages/privacy.vue`)
    - 수집항목: 관리자(이름/이메일/전화/사업자정보), 고객(이름/전화/카카오ID)
    - 제3자 제공: 카카오 OAuth, 향후 PG사
    - 개인정보보호위원회 표준양식 활용
- [ ] 환불 정책 페이지 신규 생성 (`src/pages/refund-policy.vue`)
    - public 레이아웃, 구독 환불/취소 정책
- [ ] 푸터(PublicFooter.vue)에 환불정책 링크 추가

**백엔드 요청:** 없음 (프론트엔드 정적 페이지)

---

### A-2. 이메일 발송 연동 검증

**현황:** `forgot-password.vue`, `reset-password.vue` UI 완성. `authApi.forgotPassword()`, `authApi.resetPassword()` API 함수 존재. 백엔드 이메일 발송 미연동.

**작업:**
- [ ] 백엔드 이메일 발송 연동 후, forgot-password → reset-password 플로우 E2E 검증
- [ ] 에러 케이스 검증: 미등록 이메일, 만료된 토큰, 잘못된 토큰
- [ ] 성공 시 UX 확인: 이메일 발송 완료 메시지, 재발송 방지(쿨다운)
- [ ] console.log 디버그 코드 제거 (forgot-password.vue:51, reset-password.vue:65)

**백엔드 요청사항:**
```
[요청] 이메일 발송 기능 연동
- POST /api/auth/forgot-password { email }
  → 비밀번호 재설정 링크 이메일 발송
  → 프론트 리셋 URL: {origin}/reset-password?token={token}
- POST /api/auth/reset-password { token, newPassword }
  → 토큰 검증 후 비밀번호 변경

에러 코드 확인 필요:
- 미등록 이메일 → 어떤 응답? (보안상 "발송완료"로 통일 권장)
- 토큰 만료 → 에러 코드?
- 토큰 무효 → 에러 코드?
```

---

### A-3. 슬러그(slug) 예약 페이지 E2E 검증

**현황:** 슬러그 설정/변경 UI 완성, 예약 플로우 구현 완성. 통합 검증 필요.

**작업:**
- [ ] 매장 설정에서 슬러그 설정 → `/booking/{slug}` 접근 검증
- [ ] 슬러그 미설정 매장의 디폴트 처리 확인 (에러 페이지? 안내 메시지?)
- [ ] 매장 상세 → 서비스 선택 → 날짜/시간 → 예약 완료 전체 플로우 검증
- [ ] 비회원 예약 → 예약번호+전화번호 조회 검증
- [ ] 회원(카카오 로그인) 예약 → 내 예약 목록 확인 검증
- [ ] SEO 메타태그 추가 (og:title, og:description, og:image)

**백엔드 요청사항:**
```
[확인] GET /api/businesses/{businessId} 응답에 slug 필드 포함 여부
- 이전에 누락 이슈 발견된 적 있음
- 매장 설정 저장 후 slug가 정상 반환되는지 확인 필요

[확인] GET /api/public/businesses/{slug}
- 슬러그 미존재 시 404 응답 확인
- 비활성 매장 슬러그 접근 시 처리 확인
```

---

### A-4. 관리자 프로필 기능 검증

**현황:** ProfileDialog.vue로 4탭(기본정보/비밀번호/SNS/탈퇴) 구현 완료. UserProfile.vue 메뉴에서 접근 가능.

**작업:**
- [ ] 프로필 기본정보 수정 (이름, 전화번호) API 연동 검증
- [ ] 프로필 이미지 업로드 검증
- [ ] 비밀번호 변경 (`authApi.changePassword()`) 연동 검증
- [ ] 회원탈퇴 (`authApi.deleteAccount()`) 플로우 검증
    - 확인 다이얼로그, 비밀번호 재입력, 탈퇴 후 로그아웃 처리
- [ ] SNS 계정 연결 상태 표시 (`authApi.getSocialAccounts()`) 검증

**백엔드 요청사항:**
```
[확인] 다음 API들의 정상 동작 검증 필요:
- PATCH /api/auth/profile { name, phone }
- POST /api/auth/profile/image (multipart)
- POST /api/auth/change-password { currentPassword, newPassword }
- DELETE /api/auth/account { password, reason }
- GET /api/auth/social-accounts
- DELETE /api/auth/social-accounts/{provider}

[확인] 프로필 이미지 저장 경로
- 현재 로컬(C:/Project/uploads/) → 프로덕션에서 접근 가능한지?
- Object Storage 전환 시 URL 패턴 변경 여부
```

---

## Phase B: 코드 안정화

> 기술 부채 정리 + 프로덕션 품질 확보

### B-1. console.log / 디버그 코드 전수 제거

**현황:** stores/ 디렉토리에 23개 발견

**작업:**
- [ ] `stores/auth.js` — console.log 제거 (10개)
    - 로그인 성공 로그 (라인 94), 회원가입 성공 로그 (라인 176) 등
    - console.error는 프로덕션 로거로 대체 또는 유지 판단
- [ ] `stores/reservation.js` — console.log 제거 (13개)
    - "✅ 예약 완료 응답" 디버그 로그 (라인 272) 반드시 제거
    - businessId 누락 에러는 console.error 유지 가능
- [ ] `pages/forgot-password.vue` — console.log 제거 (라인 51)
- [ ] `pages/reset-password.vue` — console.log 제거 (라인 65)

**백엔드 요청:** 없음

---

### B-2. Materio 미사용 코드 정리

**작업:**
- [ ] `src/components/dialogs/` 내 미사용 다이얼로그 12개 삭제
    - 유지: `ConfirmDeleteDialog.vue`, `ConfirmDialog.vue`
    - 삭제 대상: AddAuthenticatorAppDialog, AddEditAddressDialog, AddEditPermissionDialog, AddEditRoleDialog, AddPaymentMethodDialog, CardAddEditDialog, CreateAppDialog, EnableOneTimePasswordDialog, PaymentProvidersDialog, PricingPlanDialog, ReferAndEarnDialog, ShareProjectDialog, TwoFactorAuthDialog, UserInfoEditDialog, UserUpgradePlanDialog
- [ ] `src/navigation/horizontal/` 미사용 파일 정리 (Vertical만 사용)
- [ ] `src/composables/useApi.js` 제거 검토 (NavSearchBar만 참조)
- [ ] 루트의 `nul` 파일 삭제
- [ ] `ConfirmDialog.vue` (Materio 원본, isDialogVisible 패턴) → 사용처 확인 후 프로젝트 표준(modelValue) 통일 또는 삭제

**백엔드 요청:** 없음

---

### B-3. login.vue 보안 정리

**작업:**
- [ ] 테스트 계정 빠른 로그인 기능 제거 또는 `VITE_DEV_MODE` 환경변수로 제어
- [ ] .env.example 파일 작성 (필요한 환경변수 문서화)

```env
# .env.example
VITE_API_BASE_URL=http://localhost:8080/api
VITE_APP_TITLE=moer 예약 관리 시스템
VITE_APP_SHORT_NAME=moer
VITE_PORT=5173
# VITE_DEV_MODE=true  # 개발 모드 (테스트 계정 표시)
```

**백엔드 요청:** 없음

---

### B-4. 에러 페이지 정비

**현황:** 404 페이지 존재 (`[...error].vue`), 500 에러 페이지 없음

**작업:**
- [ ] 500 에러 페이지 생성 (또는 범용 에러 페이지로 404를 확장)
- [ ] API 에러 시 글로벌 에러 핸들링 개선 (axios 인터셉터)
    - 네트워크 에러 → 사용자 친화적 메시지
    - 401 토큰 만료 → 자동 갱신 실패 시 로그인 안내
    - 500 서버 에러 → "잠시 후 다시 시도" 메시지

**백엔드 요청:** 없음

---

### B-5. 연락처/사업자 정보 확정 반영

**현황:** `faq.vue`, `support.vue`에 `1588-XXXX` placeholder, `PublicFooter.vue`에 임시 정보

**작업:**
- [ ] 실제 연락처 확정 후 반영 (전화번호, 이메일, 사업자등록번호)
- [ ] faq.vue 연락처 업데이트
- [ ] support.vue 연락처 업데이트
- [ ] PublicFooter.vue 사업자 정보 업데이트

**백엔드 요청:** 없음 (사업 결정 사항)

---

### B-6. 고객 페이지 인증 가드 일관성 개선

**현황:** `/booking/*` 페이지는 라우터 가드가 아닌 각 페이지 `onMounted`에서 직접 인증 체크

**작업:**
- [ ] 인증 필요한 고객 페이지 식별: `my-reservations.vue`, `profile.vue`
- [ ] 라우터 가드에 고객 인증 로직 추가 또는 현재 방식 유지 결정
    - 옵션 A: `meta.requiresCustomerAuth` 메타 추가 → 가드에서 처리
    - 옵션 B: 현재 방식 유지 (각 페이지 자체 체크) — 단순하지만 불일관
- [ ] 미인증 고객이 보호 페이지 접근 시 `/booking/login`으로 리다이렉트 + 복귀 경로 보존

**백엔드 요청:** 없음

---

## Phase C: SNS 로그인 연동 검증

> 백엔드 설정 후 프론트에서 검증

### C-1. 카카오 OAuth 관리자 로그인 검증

**현황:** 고객 카카오 로그인 완성. 관리자 카카오 로그인 UI 존재하나 백엔드 연동 미테스트.

**작업:**
- [ ] login.vue의 카카오 로그인 버튼 클릭 → OAuth 플로우 검증
- [ ] oauth2-redirect.vue에서 `loginType=admin` 분기 검증
- [ ] 최초 가입 vs 기존 계정 연결 케이스 검증
- [ ] 에러 케이스: 카카오 인증 취소, 이미 연결된 계정

**백엔드 요청사항:**
```
[요청] 관리자 카카오 OAuth 설정 확인
- GET /oauth2/authorize/kakao?loginType=admin
  → 카카오 인증 후 /oauth2-redirect로 콜백
  → loginType=admin 쿠키 기반 분기 동작 확인

[확인] 관리자 최초 카카오 로그인 시 동작
- 기존 이메일 계정에 카카오 연결? → 자동 연결 또는 수동 연결?
- 카카오로만 가입(이메일 없는 경우) → 허용 여부?
```

---

### C-2. 네이버 OAuth 연동

**작업:**
- [ ] login.vue 네이버 로그인 버튼 동작 확인
- [ ] OAuth 콜백 처리 검증
- [ ] 프로필 SNS 탭에서 네이버 연결/해제 검증

**백엔드 요청사항:**
```
[요청] 네이버 OAuth 설정
- 네이버 개발자센터 앱 등록 필요
- Redirect URI: {origin}/oauth2-redirect
- GET /oauth2/authorize/naver?loginType=admin
- 응답 형식: 카카오와 동일한 토큰 응답 구조
```

---

### C-3. 구글 OAuth 연동

**작업:**
- [ ] login.vue 구글 로그인 버튼 동작 확인
- [ ] OAuth 콜백 처리 검증
- [ ] 프로필 SNS 탭에서 구글 연결/해제 검증

**백엔드 요청사항:**
```
[요청] 구글 OAuth 설정
- Google Cloud Console 앱 등록 필요
- Redirect URI: {origin}/oauth2-redirect
- GET /oauth2/authorize/google?loginType=admin
- 응답 형식: 카카오와 동일한 토큰 응답 구조
```

---

## Phase D: 베타 배포 전 최종 검증

> 실사용 환경에서의 통합 검증

### D-1. 핵심 플로우 E2E 체크리스트

**관리자 플로우:**
- [ ] 회원가입 → 온보딩 위저드 → 매장 설정 → 서비스/스태프 등록
- [ ] 예약 생성 → 확정 → 완료 (캘린더 + 목록 양쪽 확인)
- [ ] 고객 등록 → 예약 연결 → 고객 상세에서 이력 확인
- [ ] 통계 페이지 데이터 정합성 (예약/매출/고객 수치 일치)
- [ ] 리뷰 답변 작성 → 공개 페이지 반영 확인
- [ ] 구독 사용량 표시 (스태프/예약 제한) 정확성

**고객 플로우:**
- [ ] `/booking` 매장 검색 → 매장 상세 → 서비스 선택 → 예약 완료
- [ ] 비회원 예약 조회 (예약번호 + 전화번호)
- [ ] 카카오 로그인 → 내 예약 목록 → 예약 취소
- [ ] 리뷰 작성 (완료된 예약만 가능한지 확인)
- [ ] 모바일 반응형 전체 확인 (하단 네비게이션, 스테퍼 등)

**슈퍼관리자 플로우:**
- [ ] 매장 목록 → 매장 선택 → 해당 매장 관리자 화면 전환
- [ ] 사용자 정지/역할 변경 → 실제 로그인 제한 확인

---

### D-2. 성능/UX 점검

- [ ] 초기 로딩 속도 확인 (번들 사이즈)
- [ ] 이미지 최적화 (랜딩 페이지 Hero 등)
- [ ] 모바일 터치 영역 충분한지 (최소 44px)
- [ ] 폼 유효성 검사 메시지 한글 통일

---

## Phase E: 수익화 - PG 결제 연동

> PG사 등록 + 백엔드 완료 후 프론트 작업

### E-1. 결제 위젯 연동

**현황:** `payment/index.vue` UI 존재, PG사 미연결

**작업:**
- [ ] PG SDK 설치 (Toss Payments 등)
- [ ] 결제 위젯 컴포넌트 생성
- [ ] 결제 진행 → 성공/실패 처리
- [ ] 결제 내역 페이지 실데이터 연동 검증

**백엔드 요청사항:**
```
[요청] PG 결제 API
- POST /api/payments { planId, couponCode? }
  → PG 결제 키 발급
- POST /api/payments/confirm { paymentKey, orderId, amount }
  → 결제 승인 (PG 웹훅과 병행)
- POST /api/payments/{id}/refund { reason }
  → 환불 처리
- GET /api/payments
  → 결제 내역 목록

[요청] PG 웹훅 엔드포인트
- POST /api/payments/webhook
  → PG사에서 결제 상태 변경 시 호출
```

---

### E-2. 정기결제 (구독 자동갱신)

**작업:**
- [ ] 구독 관리 페이지에서 결제 수단 등록/변경 UI
- [ ] 자동 갱신 ON/OFF 토글
- [ ] 다음 결제일 표시
- [ ] 결제 실패 시 알림 + 재시도 UI

**백엔드 요청사항:**
```
[요청] 정기결제 API
- POST /api/subscription/billing-key { ... }
  → 빌링키 등록 (카드 정보)
- PATCH /api/subscription/auto-renew { enabled: boolean }
  → 자동갱신 설정
- GET /api/subscription
  → nextBillingDate, autoRenew, paymentMethod 필드 확인
```

---

## Phase F: 서비스 고도화 (Post-Beta)

> 베타 이후 피드백 기반 추가 기능

### F-1. 카카오 알림톡 설정 페이지

**작업:**
- [ ] 매장 설정에 "알림 설정" 탭/페이지 추가
- [ ] 알림 종류별 ON/OFF 토글 (예약 확정, D-1 리마인더, 변경/취소)
- [ ] 알림 발송 이력 페이지 연동 강화 (현재 notification-logs)

**백엔드 요청사항:**
```
[요청] 알림 설정 API
- GET /api/businesses/{id}/notification-settings
  → 알림 종류별 활성화 상태
- PATCH /api/businesses/{id}/notification-settings
  → { reservationConfirm: true, dayBeforeReminder: true, ... }

[요청] 카카오 알림톡 발송 연동
- 카카오 비즈니스 채널 개설 필요
- 알림톡 템플릿 심사 (3~5 영업일)
- 예약 확정/취소/리마인더 자동 발송
```

---

### F-2. 매출 리포트 다운로드

**작업:**
- [ ] 통계 페이지에 "PDF 다운로드" / "Excel 다운로드" 버튼 추가
- [ ] 기간 선택 후 리포트 생성

**백엔드 요청사항:**
```
[요청] 리포트 다운로드 API
- GET /api/businesses/{id}/statistics/report?type=revenue&period=monthly&format=pdf
  → PDF/Excel 파일 응답
```

---

### F-3. 실시간 알림 (WebSocket)

**현황:** 폴링 기반 (30초 간격)으로 동작 중

**작업:**
- [ ] WebSocket 또는 SSE 연결 구현
- [ ] `useNotificationStore`의 폴링 → WebSocket 전환
- [ ] 브라우저 푸시 알림 (Notification API) 추가

**백엔드 요청사항:**
```
[요청] WebSocket 또는 SSE 엔드포인트
- WS /ws/notifications
  또는
- GET /api/notifications/stream (SSE)
  → 실시간 알림 이벤트 스트림
```

---

## 작업 우선순위 요약

```
[1주차] Phase A: 출시 차단 이슈
  A-1  약관/개인정보/환불정책 정식 버전
  A-2  이메일 발송 검증 (백엔드 연동 후)
  A-3  슬러그 예약 E2E 검증
  A-4  관리자 프로필 기능 검증

[2주차] Phase B: 코드 안정화
  B-1  console.log 제거
  B-2  Materio 미사용 코드 정리
  B-3  login.vue 보안 + .env.example
  B-4  에러 페이지 정비
  B-5  연락처/사업자 정보 (확정 후)
  B-6  고객 페이지 인증 가드

[3주차] Phase C: SNS 로그인 검증
  C-1  카카오 OAuth 관리자
  C-2  네이버 OAuth
  C-3  구글 OAuth

[4~5주차] Phase D: 베타 배포 전 최종 검증
  D-1  핵심 플로우 E2E 체크리스트
  D-2  성능/UX 점검

[6~7주차] Phase E: PG 결제 연동
  E-1  결제 위젯
  E-2  정기결제

[Post-Beta] Phase F: 서비스 고도화
  F-1  카카오 알림톡
  F-2  매출 리포트 다운로드
  F-3  실시간 알림 (WebSocket)
```

---

## 백엔드 요청사항 총정리 (우선순위순)

### 즉시 필요 (Phase A)

| # | 요청 | API | 상태 |
|---|------|-----|------|
| 1 | 이메일 발송 연동 | POST /auth/forgot-password, /reset-password | API 존재, 이메일 발송 미구현 |
| 2 | 매장 상세 응답에 slug 필드 포함 확인 | GET /businesses/{id} | 이전 누락 이슈 |
| 3 | 비활성 매장 슬러그 접근 처리 | GET /public/businesses/{slug} | 처리 방식 확인 |
| 4 | 프로필 이미지 저장 경로 | POST /auth/profile/image | Object Storage 전환 여부 |

### 2주 내 필요 (Phase B-C)

| # | 요청 | API | 상태 |
|---|------|-----|------|
| 5 | 카카오 OAuth 관리자 로그인 | GET /oauth2/authorize/kakao?loginType=admin | 설정 확인 |
| 6 | 네이버 OAuth 설정 | GET /oauth2/authorize/naver | 신규 구현 |
| 7 | 구글 OAuth 설정 | GET /oauth2/authorize/google | 신규 구현 |

### 베타 후 (Phase E-F)

| # | 요청 | API | 상태 |
|---|------|-----|------|
| 8 | PG 결제 API | POST /payments, /payments/confirm | 신규 구현 |
| 9 | 정기결제 빌링키 | POST /subscription/billing-key | 신규 구현 |
| 10 | 알림 설정 API | GET/PATCH /businesses/{id}/notification-settings | 신규 구현 |
| 11 | 카카오 알림톡 발송 | 내부 연동 | 신규 구현 |
| 12 | WebSocket/SSE | WS /ws/notifications | 신규 구현 |
