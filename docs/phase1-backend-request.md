# Phase 1: 백엔드 API 요청서 — 즉시 수정 + 핵심 보완

> 서비스 출시 전 필수 항목
> 작성일: 2026-02-14

---

## 현재 보유 API 현황 (참고)

| 모듈 | 파일 | 주요 엔드포인트 |
|------|------|----------------|
| Auth | `src/api/auth.js` | login, logout, register, refreshToken, getCurrentUser, changePassword, forgotPassword, resetPassword |
| Customers | `src/api/customers.js` | CRUD + search + VIP/new/regular 필터 |
| Services | `src/api/services.js` | CRUD + toggle-active |
| Staffs | `src/api/staffs.js` | CRUD + toggle-active + 예약조회 |
| Reservations | `src/api/reservations.js` | CRUD + 상태변경 + 날짜/기간 조회 |
| Business Settings | `src/api/business-settings.js` | 매장정보 + 설정 + 휴무일 CRUD |
| Payment | `src/api/payment.js` | 결제생성/환불/조회 |
| Coupon | `src/api/coupon.js` | CRUD + validate |
| Subscription | `src/api/subscription.js` | 조회 + 플랜변경 + 취소 |
| Service Categories | `src/api/service-categories.js` | CRUD + 정렬 |

---

## 1. 프로필/계정 관리 API (신규)

> 현재 `/auth/me` 로 사용자 정보 조회만 가능. 프로필 수정/탈퇴 API 없음

### 1-1. 프로필 정보 수정

```
PATCH /api/auth/profile
Authorization: Bearer {token}

Request Body:
{
  "name": "홍길동",           // 선택
  "phone": "010-1234-5678",   // 선택
  "profileImage": "base64..." // 선택, 아래 1-2와 택 1
}

Response 200:
{
  "data": {
    "id": 1,
    "email": "user@example.com",  // 이메일은 수정 불가
    "name": "홍길동",
    "phone": "010-1234-5678",
    "profileImageUrl": "/uploads/profiles/1.jpg",
    "role": "ADMIN",
    "businessId": 1,
    "createdAt": "2026-01-15T10:00:00"
  }
}

Error:
- 400 Bad Request: 유효하지 않은 필드값
```

### 1-2. 프로필 이미지 업로드

```
POST /api/auth/profile/image
Authorization: Bearer {token}
Content-Type: multipart/form-data

Request:
  file: (이미지 파일, max 5MB, jpg/png/webp)

Response 200:
{
  "data": {
    "profileImageUrl": "/uploads/profiles/1_20260214.jpg"
  }
}

Error:
- 413: 파일 크기 초과
- 415: 지원하지 않는 파일 형식
```

### 1-3. 비밀번호 변경 (이미 존재 — 확인 필요)

```
POST /api/auth/change-password
Authorization: Bearer {token}

Request Body:
{
  "currentPassword": "oldPw123",
  "newPassword": "newPw456",
  "confirmPassword": "newPw456"
}

Response 200:
{ "message": "비밀번호가 변경되었습니다." }

Error:
- A010: 현재 비밀번호 불일치
- A011: 새 비밀번호 정책 미충족 (8자 이상, 영문+숫자)
```

> **확인 요청**: `auth.changePassword()`는 프론트에 정의되어 있으나, 백엔드 실제 구현 여부 및 에러 코드 확인 필요

### 1-4. 회원 탈퇴

```
DELETE /api/auth/account
Authorization: Bearer {token}

Request Body:
{
  "password": "currentPw",     // 비밀번호 확인
  "reason": "서비스 불만족"     // 선택, 탈퇴 사유
}

Response 200:
{ "message": "회원 탈퇴가 완료되었습니다." }

Side Effects:
- 사용자 상태 → DELETED (soft delete)
- 30일 후 완전 삭제 또는 즉시 삭제 (정책 결정 필요)
- 연결된 매장 데이터 처리 정책 필요:
  - 매장 소유자가 탈퇴 시 → 매장도 비활성화?
  - 직원이 탈퇴 시 → 스태프 비활성화만?

Error:
- A010: 비밀번호 불일치
- A012: 탈퇴 불가 (진행 중인 예약 있음 등)
```

### 1-5. SNS 연결 계정 목록 조회

```
GET /api/auth/social-accounts
Authorization: Bearer {token}

Response 200:
{
  "data": [
    { "provider": "GOOGLE", "email": "user@gmail.com", "connectedAt": "2026-01-20T10:00:00" },
    { "provider": "KAKAO", "nickname": "홍길동", "connectedAt": "2026-01-22T14:00:00" }
  ]
}
```

### 1-6. SNS 계정 연결 해제

```
DELETE /api/auth/social-accounts/{provider}
Authorization: Bearer {token}

Path: provider = GOOGLE | KAKAO | NAVER

Response 200:
{ "message": "Google 계정 연결이 해제되었습니다." }

Error:
- A013: 비밀번호 미설정 상태에서 마지막 로그인 수단 해제 불가
```

---

## 2. OAuth/SNS 로그인 백엔드 설정 (기존 확인/설정)

> 프론트엔드 UI(버튼 + oauth2-redirect)는 완성 상태. 백엔드 OAuth 설정 필요

### 필요 작업

| Provider | Client ID 등록 | Redirect URI | 상태 |
|----------|---------------|--------------|------|
| Google | Google Cloud Console | `{domain}/oauth2-redirect` | **확인 필요** |
| Kakao | Kakao Developers | `{domain}/oauth2-redirect` | **확인 필요** |
| Naver | Naver Developers | `{domain}/oauth2-redirect` | **확인 필요** |

### 프론트엔드가 기대하는 OAuth 플로우

```
1. 프론트: window.location.href = '/api/oauth2/authorize/{provider}?redirect_uri={frontendUrl}/oauth2-redirect'
2. 백엔드: OAuth Provider로 리다이렉트
3. Provider: 인증 후 백엔드 callback URL로 리다이렉트
4. 백엔드: JWT 발급 후 프론트 redirect_uri로 리다이렉트
   → {frontendUrl}/oauth2-redirect?token={accessToken}&refreshToken={refreshToken}
5. 프론트: URL 파라미터에서 토큰 추출 → localStorage 저장 → /shop-admin/dashboard 이동
```

### 확인 필요 사항
- [ ] OAuth2 Provider별 application.yml 설정 완료 여부
- [ ] 기존 계정과 SNS 계정 자동 연결 (동일 이메일 기반) 정책
- [ ] 첫 SNS 로그인 시 자동 회원가입 처리 여부
- [ ] 매장(Business) 자동 생성 여부 (SNS 회원가입 시)

---

## 3. 비밀번호 찾기 이메일 발송 (기존 API + SMTP 설정)

> 프론트엔드 `forgot-password.vue`, `reset-password.vue` UI 완성 상태
> API 엔드포인트 `POST /auth/forgot-password`, `POST /auth/reset-password` 정의 완료

### 백엔드 필요 작업

```
1. SMTP 설정 (네이버 웍스 또는 기타)
   - host: smtp.worksmobile.com (네이버 웍스) 또는 smtp.gmail.com
   - port: 587 (TLS)
   - username: noreply@yemo.io
   - password: ****

2. 이메일 템플릿
   - 제목: "[YEMO] 비밀번호 재설정 안내"
   - 본문: 재설정 링크 포함 (유효시간 30분)
   - 링크: {frontendUrl}/reset-password?token={resetToken}

3. 보안 정책
   - 토큰 유효 시간: 30분
   - 동일 이메일 발송 제한: 5분 간격
   - 토큰 1회 사용 후 즉시 무효화
```

### 프론트엔드 기대 동작

```
forgot-password.vue:
  POST /api/auth/forgot-password { email: "user@example.com" }
  → 성공: { message: "이메일로 재설정 링크가 발송되었습니다." }
  → 실패(미등록 이메일): 보안상 동일 성공 메시지 반환 권장

reset-password.vue:
  POST /api/auth/reset-password { token: "abc123", newPassword: "newPw456" }
  → 성공: { message: "비밀번호가 재설정되었습니다." }
  → 실패: A020(만료된 토큰), A021(유효하지 않은 토큰)
```

---

## 4. 알림 시스템 최소 구현 (또는 제거)

> NavBarNotifications.vue에 Materio 영문 더미 데이터 6개 하드코딩
> Phase 1에서는 **두 가지 옵션** 중 선택

### 옵션 A: 더미 제거 (최소 공수)
- 프론트엔드 작업만으로 해결
- NavBarNotifications에서 하드코딩 데이터 제거
- "알림 없음" 빈 상태 표시
- **백엔드 작업 없음**

### 옵션 B: 기본 알림 API 구현 (권장)

```
GET /api/notifications
Authorization: Bearer {token}

Query Params:
  page: 1
  size: 20
  unreadOnly: false

Response 200:
{
  "data": {
    "items": [
      {
        "id": 1,
        "type": "RESERVATION_NEW",        // RESERVATION_NEW, RESERVATION_CANCEL, SYSTEM, etc.
        "title": "새 예약이 등록되었습니다",
        "message": "김미소 고객님이 내일 14:00 커트 예약",
        "isRead": false,
        "createdAt": "2026-02-14T10:30:00",
        "link": "/shop-admin/reservations/list"  // 클릭 시 이동 경로
      }
    ],
    "totalCount": 15,
    "unreadCount": 3
  }
}
```

```
PATCH /api/notifications/{id}/read
Authorization: Bearer {token}

Response 200:
{ "message": "읽음 처리되었습니다." }
```

```
PATCH /api/notifications/read-all
Authorization: Bearer {token}

Response 200:
{ "message": "모든 알림을 읽음 처리했습니다." }
```

> Phase 1에서는 REST polling으로 구현. WebSocket/SSE는 Phase 4에서 도입

---

## 5. 프론트엔드 Only 작업 (백엔드 불필요)

> 아래 항목은 백엔드 변경 없이 프론트엔드만으로 해결

| 작업 | 설명 |
|------|------|
| `alert()` → `useSnackbar` 전환 (14곳) | 에러 처리 UI 통일 |
| ReservationDialog.vue 정리 | 하드코딩 staffOptions/serviceOptions를 store에서 가져오도록 수정 |
| NavBar 알림 더미 제거 (옵션 A 선택 시) | 하드코딩 데이터 제거 |
| 약관/정책 정식 버전 | 법률 검토 후 텍스트 교체 |

---

## 6. 우선순위 요약

| 순번 | 작업 | 담당 | 의존성 |
|------|------|------|--------|
| 1 | 프로필 수정 API (1-1, 1-2) | 백엔드 | 없음 |
| 2 | 비밀번호 변경 확인 (1-3) | 백엔드 | 없음 |
| 3 | 회원 탈퇴 API (1-4) | 백엔드 | 탈퇴 정책 결정 |
| 4 | SMTP 설정 + 이메일 발송 (3) | 백엔드 | SMTP 계정 |
| 5 | OAuth 설정 확인 (2) | 백엔드 | Provider 계정 |
| 6 | SNS 연결 관리 API (1-5, 1-6) | 백엔드 | OAuth 설정 후 |
| 7 | 알림 API (4-B) or 더미 제거 (4-A) | 백엔드/프론트 | 정책 결정 |
| 8 | alert→snackbar, ReservationDialog 등 | 프론트 | 없음 |
