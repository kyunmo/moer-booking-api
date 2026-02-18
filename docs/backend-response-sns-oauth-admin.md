# 백엔드 응답: 카카오/네이버/구글 관리자 OAuth 로그인

> **작성일:** 2026-02-18
> **상태:** 백엔드 구현 완료 + 리다이렉트 연동 수정 완료
> **요청 문서:** `backend-request-b2-kakao-admin-oauth.md`, `backend-request-b3-naver-admin-oauth.md`, `backend-request-b4-google-admin-oauth.md`

---

## 1. 구현 현황 요약

### 결론: 3개 제공자 모두 백엔드 구현 완료

| 항목 | 카카오 | 네이버 | 구글 |
|------|--------|--------|------|
| OAuth2 클라이언트 설정 (application.yml) | ✅ | ✅ | ✅ |
| CustomOAuth2UserService (사용자 정보 추출) | ✅ | ✅ | ✅ |
| loginType 분기 (admin/customer) | ✅ | ✅ | ✅ |
| JWT 토큰 발급 (SuccessHandler) | ✅ | ✅ | ✅ |
| 에러 처리 (FailureHandler) | ✅ | ✅ | ✅ |
| 신규 사용자 자동 생성 | ✅ | ✅ | ✅ |
| 기존 이메일 계정 자동 연동 | ✅ | ✅ | ✅ |
| SNS 계정 연결/해제 API | ✅ | ✅ | ✅ |

---

## 2. 이번에 수정한 사항 (리다이렉트 연동)

### 2-1. 리다이렉트 URL 변경

**수정 전:**
```
성공: http://localhost:3000/oauth2/redirect?accessToken=...&refreshToken=...&isNewUser=...
실패: http://localhost:3000/oauth2/redirect?error=oauth2_failed&message=...
```

**수정 후:**
```
성공: http://localhost:3000/oauth2-redirect?accessToken=...&refreshToken=...&loginType=admin&isNewUser=...
실패: http://localhost:3000/oauth2-redirect?error=true&message=...&loginType=admin
```

### 2-2. 변경 상세

| 항목 | 수정 전 | 수정 후 |
|------|--------|--------|
| 리다이렉트 경로 | `/oauth2/redirect` | `/oauth2-redirect` |
| 고객 리다이렉트 경로 | `/oauth2/customer-redirect` | `/oauth2-redirect` (동일) |
| 성공 시 loginType 파라미터 | 미포함 | `loginType=admin` 또는 `loginType=customer` 포함 |
| 실패 시 loginType 파라미터 | 미포함 | `loginType=admin` 또는 `loginType=customer` 포함 |
| 에러 파라미터 | `error=oauth2_failed` | `error=true` |

### 2-3. 수정 파일

| 파일 | 변경 내용 |
|------|----------|
| `application.yml` | `redirect-uri`, `customer-redirect-uri`를 `/oauth2-redirect`로 변경 |
| `OAuth2AuthenticationSuccessHandler.java` | `loginType` 쿼리 파라미터 추가 |
| `OAuth2AuthenticationFailureHandler.java` | `loginType` 파라미터 추가, `error=true`로 변경, 쿠키 읽기/삭제 로직 추가 |

---

## 3. API 엔드포인트

### 3-1. OAuth 인가 요청

```
GET /oauth2/authorize/{provider}?loginType=admin
```

| 제공자 | URL |
|--------|-----|
| 카카오 | `GET /oauth2/authorize/kakao?loginType=admin` |
| 네이버 | `GET /oauth2/authorize/naver?loginType=admin` |
| 구글   | `GET /oauth2/authorize/google?loginType=admin` |

> `loginType=customer`로 고객 로그인도 가능 (동일 엔드포인트)

### 3-2. OAuth 콜백 (Spring Security 자동 처리)

```
GET /login/oauth2/code/{provider}
```

이 엔드포인트는 직접 호출하지 않음. SNS 제공자가 인증 후 자동으로 리다이렉트.

### 3-3. 프론트엔드 리다이렉트 (인증 완료 후)

**성공:**
```
{frontendUrl}/oauth2-redirect
  ?accessToken={JWT_ACCESS_TOKEN}
  &refreshToken={JWT_REFRESH_TOKEN}
  &loginType=admin
  &isNewUser=true|false
```

**실패:**
```
{frontendUrl}/oauth2-redirect
  ?error=true
  &message={에러메시지}
  &loginType=admin
```

### 3-4. SNS 연결 관리 API (기존 구현)

```
GET    /api/auth/social-accounts              → SNS 연결 목록
DELETE /api/auth/social-accounts/{provider}    → SNS 연결 해제
```

**응답 예시 (GET /api/auth/social-accounts):**
```json
{
  "success": true,
  "data": [
    {
      "provider": "KAKAO",
      "email": "user@kakao.com",
      "name": "홍길동",
      "connectedAt": "2026-02-18 14:30:00"
    },
    {
      "provider": "GOOGLE",
      "email": "user@gmail.com",
      "name": "홍길동",
      "connectedAt": "2026-02-18 15:00:00"
    }
  ]
}
```

---

## 4. 신규 관리자 OAuth 로그인 동작

### 4-1. 기존 계정이 있는 경우 (이메일 일치)

1. SNS에서 받은 이메일로 기존 users 테이블 조회
2. 기존 계정에 SNS 계정 자동 연동 (sns_accounts에 저장)
3. 기존 역할 유지 (OWNER/ADMIN/STAFF)
4. JWT 토큰 발급 후 프론트엔드 리다이렉트

### 4-2. 완전 신규 계정 (이메일 미존재)

1. `loginType=admin`이면 → **OWNER** 역할로 자동 생성
   - 30일 체험판 자동 설정
   - 기본 매장 자동 생성 (`{이름}님의 매장`)
   - `isNewUser=true`로 리다이렉트
2. `loginType=customer`이면 → **CUSTOMER** 역할로 자동 생성
   - 매장 미생성
   - 체험판 미설정

### 4-3. 고객 계정으로 관리자 로그인 시도

- 현재 구현: 기존 계정 역할 유지 (역할 변경 안 함)
- 이메일이 같은 CUSTOMER 계정이 있으면 해당 계정에 SNS 연동만 추가
- 프론트엔드에서 `/api/auth/me` 호출 시 `role: CUSTOMER`로 응답됨
- 프론트엔드에서 역할 기반 라우팅으로 처리 가능

---

## 5. 에러 케이스

| 케이스 | 처리 |
|--------|------|
| SNS 인증 취소 | `error=true&message=인증 취소됨&loginType=admin` |
| 이메일 미동의 | `error=true&message=SNS에서 이메일 정보를 제공하지 않았습니다&loginType=admin` |
| SNS 서버 오류 | `error=true&message=에러내용&loginType=admin` |

**에러 코드:**

| 코드 | HTTP | 메시지 |
|------|------|--------|
| `OA001` | 400 | 지원하지 않는 SNS 제공자입니다 |
| `OA002` | 401 | SNS 로그인에 실패했습니다 |
| `OA003` | 400 | SNS에서 이메일 정보를 제공하지 않았습니다 |

---

## 6. 프론트엔드 검증 체크리스트

### 공통
- [ ] `OAUTH_BASE_URL` 계산이 올바른지 확인 (`VITE_API_BASE_URL`에서 `/api` 제거)
- [ ] `/oauth2-redirect` 페이지에서 `loginType` 파라미터 정상 수신 확인
- [ ] 관리자 로그인 성공 후 `/shop-admin/dashboard` 이동 확인
- [ ] 에러 시 메시지 표시 + `/login` 이동 버튼 확인

### 카카오
- [ ] `handleKakaoLogin()` → 카카오 동의 화면 표시
- [ ] 카카오 동의 후 → `oauth2-redirect?accessToken=...&loginType=admin`
- [ ] 기존 이메일 계정 자동 연동 확인
- [ ] 신규 계정 자동 생성 + 온보딩 플로우 확인

### 네이버
- [ ] `handleNaverLogin()` → 네이버 동의 화면 표시
- [ ] 네이버 동의 후 → `oauth2-redirect?accessToken=...&loginType=admin`
- [ ] **주의:** 개발 중 상태에서 테스터 등록 필요

### 구글
- [ ] `handleGoogleLogin()` → 구글 계정 선택 화면 표시
- [ ] 구글 선택 후 → `oauth2-redirect?accessToken=...&loginType=admin`

### SNS 프로필 관리
- [ ] ProfileDialog SNS 탭에서 연결된 SNS 목록 표시
- [ ] SNS 연결 해제 → 확인 후 삭제 (마지막 로그인 수단 삭제 방지)

---

## 7. 관리자 수동 작업 필요 사항

**별도 문서 참조:** `docs/admin-sns-oauth-setup-guide.md`

| 작업 | 긴급도 | 설명 |
|------|--------|------|
| 네이버 테스터 등록 | 즉시 | 개발 테스트를 위해 필요 |
| 카카오 운영 Redirect URI 추가 | 배포 전 | `https://yemo.kr/login/oauth2/code/kakao` |
| 네이버 운영 Callback URL 추가 | 배포 전 | `https://yemo.kr/login/oauth2/code/naver` |
| 구글 운영 리디렉션 URI 추가 | 배포 전 | `https://yemo.kr/login/oauth2/code/google` |
| 카카오 배포 심사 | 배포 전 | 약 3~5 영업일 소요 |
| 네이버 서비스 적용 신청 | 배포 전 | 약 2~3 영업일 소요 |
| 이용약관/개인정보처리방침 URL | 심사 전 | 카카오/네이버 심사 시 필요 |
| 운영 환경변수 설정 | 배포 전 | OAUTH2_REDIRECT_URI 등 |
