# 백엔드 구현 요청서 B3: 네이버 관리자 OAuth 로그인

작성일: 2026-02-18

---

## 1. 개요

네이버 OAuth를 통한 **관리자(ADMIN/OWNER) 로그인** 기능 구현을 요청합니다.

프론트엔드에서는 관리자 로그인 페이지(`src/pages/login.vue`)에 네이버 로그인 버튼이 이미 구현되어 있으며, OAuth 콜백 처리 페이지(`src/pages/oauth2-redirect.vue`)도 준비되어 있습니다.

---

## 2. 현재 프론트엔드 구현 상태

### 2-1. 관리자 로그인 페이지에서의 호출

파일: `src/pages/login.vue`

```javascript
// OAuth2 엔드포인트는 /api 접두사 없이 사용
const OAUTH_BASE_URL = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api').replace('/api', '')

function handleNaverLogin() {
  window.location.href = `${OAUTH_BASE_URL}/oauth2/authorize/naver?loginType=admin`
}
```

실제 호출 URL 예시:
```
http://localhost:8080/oauth2/authorize/naver?loginType=admin
```

### 2-2. OAuth 콜백 처리 페이지

파일: `src/pages/oauth2-redirect.vue`

프론트엔드 콜백 페이지 URL: `{frontendUrl}/oauth2-redirect`

```javascript
onMounted(async () => {
  const {
    accessToken,
    refreshToken,
    loginType,
    error: errorParam,
    message,
  } = route.query

  // loginType === 'admin' 이면 관리자 로그인 처리
  isCustomer.value = loginType === 'customer'

  if (errorParam) {
    error.value = true
    errorMessage.value = message || 'SNS 로그인에 실패했습니다.'
    return
  }

  if (!accessToken || !refreshToken) {
    error.value = true
    errorMessage.value = '로그인 정보를 받지 못했습니다.'
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

### 2-3. 에러 시 이동 경로

에러가 발생하면 `loginType` 값에 따라 로그인 페이지 분기:

```javascript
const loginRedirectPath = computed(() => {
  return isCustomer.value ? '/booking/login' : '/login'
})
```

관리자 에러 시 `/login`으로 이동합니다.

---

## 3. 필요한 백엔드 엔드포인트

### 3-1. OAuth 인가 요청 엔드포인트

```
GET /oauth2/authorize/naver?loginType=admin
```

- Spring Security OAuth2 표준 엔드포인트로 등록
- `loginType=admin` 쿼리 파라미터를 `state` 또는 세션에 보존하여 콜백 시 식별에 사용

### 3-2. OAuth 콜백 후 프론트엔드 리다이렉트

네이버 인증 완료 후 프론트엔드 콜백 페이지로 리다이렉트:

**성공 시:**
```
{frontendUrl}/oauth2-redirect?accessToken={jwt}&refreshToken={refreshJwt}&loginType=admin
```

**실패 시:**
```
{frontendUrl}/oauth2-redirect?error=true&message={에러메시지}&loginType=admin
```

---

## 4. 네이버 개발자 센터 설정

### 4-1. 애플리케이션 등록

네이버 개발자 센터 (https://developers.naver.com)에서 "Application 등록" 메뉴를 통해 애플리케이션을 생성합니다.

### 4-2. Callback URL 등록

네이버 개발자 센터의 "API 설정 > 로그인 오픈 API 서비스 환경"에서 Callback URL 등록:

| 환경 | Callback URL |
|------|-------------|
| 개발 | `http://localhost:8080/login/oauth2/code/naver` |
| 스테이징 | `https://api.staging.yemo.kr/login/oauth2/code/naver` |
| 운영 | `https://api.yemo.kr/login/oauth2/code/naver` |

네이버는 개발/운영 URL을 별도 등록해야 합니다.

### 4-3. 필요한 권한 (Scope)

네이버 로그인 동의항목:

| 권한 | 설명 | 필수 여부 |
|------|------|-----------|
| `name` | 이름 | 필수 |
| `email` | 이메일 | 필수 |
| `profile_image` | 프로필 사진 | 선택 |
| `mobile` | 휴대폰 번호 | 선택 |

### 4-4. application.yml 설정 예시

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          naver:
            client-id: ${NAVER_CLIENT_ID}
            client-secret: ${NAVER_CLIENT_SECRET}
            redirect-uri: "{baseUrl}/login/oauth2/code/naver"
            authorization-grant-type: authorization_code
            scope:
              - name
              - email
              - profile_image
            client-name: Naver
        provider:
          naver:
            authorization-uri: https://nid.naver.com/oauth2.0/authorize
            token-uri: https://nid.naver.com/oauth2.0/token
            user-info-uri: https://openapi.naver.com/v1/nid/me
            user-name-attribute: response
```

### 4-5. 네이버 사용자 정보 응답 구조 주의사항

네이버 User Info API는 다른 OAuth 제공자와 달리 중첩 구조로 응답합니다:

```json
{
  "resultcode": "00",
  "message": "success",
  "response": {
    "id": "1234567890",
    "email": "user@naver.com",
    "name": "홍길동",
    "profile_image": "https://...",
    "mobile": "010-1234-5678"
  }
}
```

Spring Security에서 `user-name-attribute: response`로 설정하고, `OAuth2UserService`에서 `response` 키 아래에서 이메일 등을 추출해야 합니다.

---

## 5. loginType 식별 방식

네이버 OAuth는 `state` 파라미터를 표준으로 지원합니다. `loginType=admin`을 state에 인코딩하여 콜백에서 식별하는 방식을 권장합니다.

```java
// CustomOAuth2AuthorizationRequestResolver 구현 예시
@Override
public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
    OAuth2AuthorizationRequest authorizationRequest = defaultResolver.resolve(request, clientRegistrationId);
    if (authorizationRequest == null) return null;

    String loginType = request.getParameter("loginType");

    return OAuth2AuthorizationRequest.from(authorizationRequest)
        .state(authorizationRequest.getState() + "|loginType=" + loginType)
        .build();
}

// 콜백 핸들러에서
String state = oAuth2AuthenticationToken.getAuthorizedClientRegistrationId();
String loginType = extractLoginTypeFromState(state); // "admin"
```

---

## 6. 관리자 계정 처리 로직

### 6-1. 신규 관리자 (첫 네이버 로그인)

네이버 이메일로 기존 계정이 없는 경우:

**옵션 A (권장):** 자동으로 ADMIN 역할의 신규 계정 생성 후 토큰 발급
- `businessId`가 null인 상태 → 프론트엔드 온보딩 플로우로 처리

**옵션 B:** 에러 응답으로 이메일 회원가입 유도
```
{frontendUrl}/oauth2-redirect?error=true&message=가입되지 않은 네이버 계정입니다.&loginType=admin
```

### 6-2. 기존 이메일 계정과 중복 처리

네이버 이메일과 동일한 이메일로 이미 이메일/비밀번호 계정이 존재하는 경우:

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

### 7-1. 에러 케이스별 message 예시

| 케이스 | message 값 |
|--------|------------|
| 네이버 인증 취소 | `네이버 로그인을 취소했습니다.` |
| 이메일 미동의 | `네이버 계정의 이메일 동의가 필요합니다.` |
| 관리자 계정 없음 | `가입되지 않은 네이버 계정입니다.` |
| 고객 계정으로 시도 | `고객 계정으로는 관리자 로그인을 할 수 없습니다.` |
| 서버 오류 | `SNS 로그인 중 오류가 발생했습니다.` |

### 7-2. 프론트엔드 에러 처리

파일: `src/pages/oauth2-redirect.vue`

```javascript
if (errorParam) {
  error.value = true
  errorMessage.value = message || 'SNS 로그인에 실패했습니다.'
  // 에러 화면 표시 후 '/login' 버튼 제공
  return
}
```

---

## 8. 네이버 특이사항

### 8-1. state 파라미터 필수

네이버 OAuth는 CSRF 방지를 위해 `state` 파라미터를 필수로 요구합니다. Spring Security가 자동으로 생성하지만, `loginType` 정보를 함께 인코딩해야 합니다.

### 8-2. 개발 환경 제한

네이버 개발자 센터에서 앱 상태가 "개발 중"인 경우, 등록된 테스터 계정만 로그인 가능합니다. 운영 배포 전 "서비스 적용" 상태로 변경하고 심사를 받아야 합니다.

### 8-3. 모바일 번호 형식

네이버에서 받은 `mobile` 필드는 `010-1234-5678` 형식(하이픈 포함)으로 제공됩니다. DB 저장 시 형식 정규화가 필요할 수 있습니다.

---

## 9. 프론트엔드 연동 포인트 요약

| 항목 | 위치 |
|------|------|
| 네이버 로그인 버튼 | `src/pages/login.vue` - `handleNaverLogin()` |
| OAuth 콜백 수신 | `src/pages/oauth2-redirect.vue` - `handleAdminLogin()` |
| 토큰 저장 | `localStorage.accessToken`, `localStorage.refreshToken` |
| 사용자 정보 조회 | `GET /api/auth/me` (로그인 후 자동 호출) |
| 로그인 후 이동 | `/shop-admin/dashboard` |
| 에러 시 이동 | `/login` |
| OAUTH_BASE_URL 계산 | `VITE_API_BASE_URL`에서 `/api` 제거한 값 |

---

## 10. 다른 OAuth 제공자와의 비교

| 항목 | 카카오 | 네이버 | 구글 |
|------|--------|--------|------|
| 개발자 콘솔 | developers.kakao.com | developers.naver.com | console.cloud.google.com |
| 이메일 제공 | scope: account_email | scope: email | scope: email (기본) |
| 사용자 정보 구조 | 중첩 (kakao_account) | 중첩 (response) | 플랫 |
| state 파라미터 | 선택 | 필수 | 선택 |
| 심사 필요 여부 | 서비스 배포 전 | 서비스 적용 신청 | 없음 (단, 민감 scope는 심사) |
