# 백엔드 구현 요청서 B2: 카카오 관리자 OAuth 로그인

작성일: 2026-02-18

---

## 1. 개요

카카오 OAuth를 통한 **관리자(ADMIN/OWNER) 로그인** 기능 구현을 요청합니다.

기존에 고객(CUSTOMER) 대상 카카오 로그인은 구현 완료된 상태이며, 이번 요청은 동일한 카카오 OAuth 플로우를 관리자 역할에도 적용하는 것입니다.

---

## 2. 현재 프론트엔드 구현 상태

### 2-1. 관리자 로그인 페이지에서의 호출

파일: `src/pages/login.vue`

```javascript
// OAuth2 엔드포인트는 /api 접두사 없이 사용
const OAUTH_BASE_URL = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api').replace('/api', '')

function handleKakaoLogin() {
  window.location.href = `${OAUTH_BASE_URL}/oauth2/authorize/kakao?loginType=admin`
}
```

실제 호출 URL 예시:
```
http://localhost:8080/oauth2/authorize/kakao?loginType=admin
```

### 2-2. OAuth 콜백 처리 페이지

파일: `src/pages/oauth2-redirect.vue`

프론트엔드 콜백 페이지 URL: `{frontendUrl}/oauth2-redirect`

```javascript
onMounted(async () => {
  const {
    accessToken,
    refreshToken,
    isNewUser,
    loginType,
    error: errorParam,
    message,
  } = route.query

  // loginType === 'admin' 이면 관리자 로그인 처리
  isCustomer.value = loginType === 'customer'

  if (errorParam) {
    // 에러 처리: error + message 파라미터 표시
    return
  }

  if (!accessToken || !refreshToken) {
    // 토큰 없음 처리
    return
  }

  // 관리자 OAuth 처리
  async function handleAdminLogin(accessToken, refreshToken) {
    localStorage.setItem('accessToken', accessToken)
    localStorage.setItem('refreshToken', refreshToken)
    authStore.accessToken = accessToken
    authStore.refreshToken = refreshToken

    await authStore.fetchCurrentUser()  // GET /api/auth/me
    router.push('/shop-admin/dashboard')
  }
})
```

### 2-3. 관리자 인증 스토어

파일: `src/stores/auth.js`

토큰은 `localStorage`의 `accessToken`, `refreshToken` 키에 저장됩니다.

OAuth 콜백 처리 후 `GET /api/auth/me`를 호출하여 사용자 정보를 가져옵니다. 응답에서 기대하는 필드:

```javascript
this.user = {
  id: data.userId,
  email: data.email,
  name: data.name,
  role: data.role,       // 'ADMIN' 또는 'OWNER'
  staffId: data.staffId,
  businessId: data.businessId,
}
```

---

## 3. 필요한 백엔드 엔드포인트

### 3-1. OAuth 인가 요청 엔드포인트

```
GET /oauth2/authorize/kakao?loginType=admin
```

- Spring Security OAuth2 표준 엔드포인트로 등록
- `loginType=admin` 쿼리 파라미터를 `state` 또는 세션에 보존하여 콜백 시 식별에 사용

### 3-2. OAuth 콜백 후 프론트엔드 리다이렉트

카카오 인증 완료 후 프론트엔드 콜백 페이지로 리다이렉트:

**성공 시:**
```
{frontendUrl}/oauth2-redirect?accessToken={jwt}&refreshToken={refreshJwt}&loginType=admin
```

**실패 시:**
```
{frontendUrl}/oauth2-redirect?error=true&message={에러메시지}&loginType=admin
```

프론트엔드에서 `loginType` 파라미터로 관리자/고객을 구분하므로 반드시 포함해야 합니다.

---

## 4. 카카오 개발자 콘솔 설정

### 4-1. 애플리케이션 등록

카카오 개발자 콘솔 (https://developers.kakao.com)

### 4-2. Redirect URI 등록

카카오 개발자 콘솔의 "카카오 로그인 > Redirect URI"에 추가:

| 환경 | Redirect URI |
|------|-------------|
| 개발 | `http://localhost:8080/login/oauth2/code/kakao` |
| 스테이징 | `https://api.staging.yemo.kr/login/oauth2/code/kakao` |
| 운영 | `https://api.yemo.kr/login/oauth2/code/kakao` |

### 4-3. 필요한 동의항목 (Scope)

| Scope | 설명 | 필수 여부 |
|-------|------|-----------|
| `profile_nickname` | 카카오 닉네임 | 필수 |
| `account_email` | 이메일 | 필수 |
| `profile_image` | 프로필 이미지 | 선택 |

### 4-4. application.yml 설정 예시

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          kakao:
            client-id: ${KAKAO_CLIENT_ID}
            client-secret: ${KAKAO_CLIENT_SECRET}
            redirect-uri: "{baseUrl}/login/oauth2/code/kakao"
            authorization-grant-type: authorization_code
            scope:
              - profile_nickname
              - account_email
              - profile_image
            client-name: Kakao
            client-authentication-method: client_secret_post
        provider:
          kakao:
            authorization-uri: https://kauth.kakao.com/oauth/authorize
            token-uri: https://kauth.kakao.com/oauth/token
            user-info-uri: https://kapi.kakao.com/v2/user/me
            user-name-attribute: id
```

---

## 5. loginType 식별 방식

프론트엔드에서 `/oauth2/authorize/kakao?loginType=admin`으로 요청하므로, 백엔드에서 `loginType` 파라미터를 OAuth state 또는 세션에 보존해야 합니다.

권장 구현 방식:

```java
// CustomAuthorizationRequestRepository 또는 CustomOAuth2AuthorizationRequestResolver에서
// loginType을 state에 인코딩하거나 세션에 저장

// 콜백 핸들러에서
String loginType = extractLoginTypeFromState(state); // "admin" 또는 "customer"

// 리다이렉트 URL 구성
String redirectUrl = loginType.equals("admin")
    ? frontendUrl + "/oauth2-redirect?accessToken=" + jwt + "&refreshToken=" + refreshJwt + "&loginType=admin"
    : frontendUrl + "/oauth2-redirect?accessToken=" + jwt + "&refreshToken=" + refreshJwt + "&loginType=customer&isNewUser=" + isNewUser;
```

---

## 6. 관리자 계정 처리 로직

### 6-1. 신규 관리자 (첫 카카오 로그인)

카카오 이메일로 기존 계정이 없는 경우:

**옵션 A (권장):** 자동으로 ADMIN 역할의 신규 계정 생성 후 토큰 발급
- 신규 관리자는 매장(Business) 등록 화면으로 유도 필요
- `businessId`가 null인 상태로 대시보드 진입 → 온보딩 플로우 처리

**옵션 B:** 에러 응답으로 이메일/비밀번호 회원가입 유도
```
{frontendUrl}/oauth2-redirect?error=true&message=카카오 계정으로 가입된 관리자 계정이 없습니다. 이메일로 회원가입 후 카카오 계정을 연동해주세요.&loginType=admin
```

### 6-2. 기존 이메일 계정과 중복 처리

카카오 이메일과 동일한 이메일로 이미 이메일/비밀번호 계정이 존재하는 경우:

- 같은 계정으로 자동 연동 (이메일 기준 병합) 권장
- 또는 에러 메시지 반환:
  ```
  {frontendUrl}/oauth2-redirect?error=true&message=이미 해당 이메일로 가입된 계정이 있습니다.&loginType=admin
  ```

### 6-3. 고객 계정과의 충돌

`loginType=admin`으로 로그인했는데 해당 이메일이 CUSTOMER 역할인 경우:
```
{frontendUrl}/oauth2-redirect?error=true&message=고객 계정으로는 관리자 로그인을 할 수 없습니다.&loginType=admin
```

---

## 7. 에러 처리

### 7-1. 프론트엔드 에러 처리 코드

파일: `src/pages/oauth2-redirect.vue`

```javascript
if (errorParam) {
  error.value = true
  errorMessage.value = message || 'SNS 로그인에 실패했습니다.'
  // 에러 화면 표시 후 '/login' 버튼 제공
  return
}
```

### 7-2. 에러 케이스별 message 예시

| 케이스 | message 값 |
|--------|------------|
| 카카오 인증 취소 | `카카오 로그인을 취소했습니다.` |
| 이메일 미동의 | `카카오 계정의 이메일 동의가 필요합니다.` |
| 관리자 계정 없음 | `가입되지 않은 카카오 계정입니다.` |
| 고객 계정으로 시도 | `고객 계정으로는 관리자 로그인을 할 수 없습니다.` |
| 서버 오류 | `SNS 로그인 중 오류가 발생했습니다.` |

---

## 8. 프론트엔드 연동 포인트 요약

| 항목 | 위치 |
|------|------|
| 카카오 로그인 버튼 | `src/pages/login.vue` - `handleKakaoLogin()` |
| OAuth 콜백 수신 | `src/pages/oauth2-redirect.vue` - `handleAdminLogin()` |
| 토큰 저장 | `localStorage.accessToken`, `localStorage.refreshToken` |
| 사용자 정보 조회 | `GET /api/auth/me` (로그인 후 자동 호출) |
| 로그인 후 이동 | `/shop-admin/dashboard` |
| 에러 시 이동 | `/login` |
| OAUTH_BASE_URL 계산 | `VITE_API_BASE_URL`에서 `/api` 제거한 값 |

---

## 9. 기존 고객 카카오 로그인과의 차이점

| 항목 | 고객 (customer) | 관리자 (admin) |
|------|----------------|---------------|
| 호출 URL | `/oauth2/authorize/kakao?loginType=customer` | `/oauth2/authorize/kakao?loginType=admin` |
| 토큰 저장 위치 | `customerAccessToken`, `customerRefreshToken` | `accessToken`, `refreshToken` |
| 콜백 후 이동 | `/booking` 또는 저장된 redirect 경로 | `/shop-admin/dashboard` |
| 신규 사용자 처리 | `isNewUser=true` 파라미터 + `/booking/profile` 이동 | 온보딩 플로우 TBD |
| 역할 | `CUSTOMER` | `ADMIN` 또는 `OWNER` |
