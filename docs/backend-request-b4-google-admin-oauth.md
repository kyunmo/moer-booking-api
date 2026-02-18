# 백엔드 구현 요청서 B4: 구글 관리자 OAuth 로그인

작성일: 2026-02-18

---

## 1. 개요

구글 OAuth를 통한 **관리자(ADMIN/OWNER) 로그인** 기능 구현을 요청합니다.

프론트엔드에서는 관리자 로그인 페이지(`src/pages/login.vue`)에 구글 로그인 버튼이 이미 구현되어 있으며, OAuth 콜백 처리 페이지(`src/pages/oauth2-redirect.vue`)도 준비되어 있습니다.

구글 OAuth는 카카오/네이버와 달리 Spring Security의 기본 제공(Built-in) Provider로 설정이 상대적으로 간단합니다.

---

## 2. 현재 프론트엔드 구현 상태

### 2-1. 관리자 로그인 페이지에서의 호출

파일: `src/pages/login.vue`

```javascript
// OAuth2 엔드포인트는 /api 접두사 없이 사용
const OAUTH_BASE_URL = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api').replace('/api', '')

function handleGoogleLogin() {
  window.location.href = `${OAUTH_BASE_URL}/oauth2/authorize/google?loginType=admin`
}
```

실제 호출 URL 예시:
```
http://localhost:8080/oauth2/authorize/google?loginType=admin
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
GET /oauth2/authorize/google?loginType=admin
```

- Spring Security OAuth2 표준 엔드포인트로 등록
- `loginType=admin` 쿼리 파라미터를 `state` 또는 세션에 보존하여 콜백 시 식별에 사용

### 3-2. OAuth 콜백 후 프론트엔드 리다이렉트

구글 인증 완료 후 프론트엔드 콜백 페이지로 리다이렉트:

**성공 시:**
```
{frontendUrl}/oauth2-redirect?accessToken={jwt}&refreshToken={refreshJwt}&loginType=admin
```

**실패 시:**
```
{frontendUrl}/oauth2-redirect?error=true&message={에러메시지}&loginType=admin
```

---

## 4. Google Cloud Console 설정

### 4-1. 프로젝트 생성 및 OAuth 클라이언트 설정

Google Cloud Console (https://console.cloud.google.com)에서:

1. 프로젝트 생성 또는 선택
2. "API 및 서비스 > OAuth 동의 화면" 설정
3. "API 및 서비스 > 사용자 인증 정보 > OAuth 2.0 클라이언트 ID" 생성
   - 애플리케이션 유형: "웹 애플리케이션"

### 4-2. Redirect URI 등록

Google Cloud Console의 "승인된 리디렉션 URI"에 추가:

| 환경 | Redirect URI |
|------|-------------|
| 개발 | `http://localhost:8080/login/oauth2/code/google` |
| 스테이징 | `https://api.staging.yemo.kr/login/oauth2/code/google` |
| 운영 | `https://api.yemo.kr/login/oauth2/code/google` |

### 4-3. 필요한 Scope

| Scope | 설명 | 필수 여부 |
|-------|------|-----------|
| `openid` | OpenID Connect 기본 | 필수 |
| `profile` | 이름, 프로필 이미지 | 필수 |
| `email` | 이메일 주소 | 필수 |

구글은 `email`, `profile`, `openid`가 기본 scope로 포함되어 있어 별도 심사 없이 사용 가능합니다.

### 4-4. application.yml 설정 예시

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
            redirect-uri: "{baseUrl}/login/oauth2/code/google"
            scope:
              - openid
              - profile
              - email
```

구글은 Spring Security에 Built-in Provider로 등록되어 있으므로 `provider` 섹션 별도 설정이 불필요합니다.

---

## 5. loginType 식별 방식

구글 OAuth는 `state` 파라미터를 지원합니다. `loginType=admin`을 state에 인코딩하여 콜백에서 식별합니다.

```java
// CustomOAuth2AuthorizationRequestResolver 구현 예시
@Component
public class CustomOAuth2AuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private final DefaultOAuth2AuthorizationRequestResolver defaultResolver;

    public CustomOAuth2AuthorizationRequestResolver(
            ClientRegistrationRepository clientRegistrationRepository) {
        this.defaultResolver = new DefaultOAuth2AuthorizationRequestResolver(
                clientRegistrationRepository, "/oauth2/authorize");
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        return customizeAuthorizationRequest(defaultResolver.resolve(request), request);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        return customizeAuthorizationRequest(
                defaultResolver.resolve(request, clientRegistrationId), request);
    }

    private OAuth2AuthorizationRequest customizeAuthorizationRequest(
            OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request) {
        if (authorizationRequest == null) return null;

        String loginType = request.getParameter("loginType");
        if (loginType == null) loginType = "admin";

        // state에 loginType 인코딩
        String originalState = authorizationRequest.getState();
        String customState = originalState + "|loginType=" + loginType;

        return OAuth2AuthorizationRequest.from(authorizationRequest)
                .state(customState)
                .build();
    }
}

// OAuth2 성공 핸들러에서 loginType 추출
String state = authentication.getAuthorizedClientRegistrationId();
// 또는 HttpSession에서 저장된 state 조회 후 파싱
String loginType = parseLoginTypeFromState(savedState); // "admin"
```

---

## 6. 관리자 계정 처리 로직

### 6-1. 신규 관리자 (첫 구글 로그인)

구글 이메일로 기존 계정이 없는 경우:

**옵션 A (권장):** 자동으로 ADMIN 역할의 신규 계정 생성 후 토큰 발급
- 구글에서 받은 `email`, `name`, `picture`(프로필 이미지) 저장
- `businessId`가 null인 상태 → 프론트엔드 온보딩 플로우로 처리

**옵션 B:** 에러 응답으로 이메일 회원가입 유도
```
{frontendUrl}/oauth2-redirect?error=true&message=가입되지 않은 구글 계정입니다.&loginType=admin
```

### 6-2. 기존 이메일 계정과 중복 처리

구글 이메일과 동일한 이메일로 이미 이메일/비밀번호 계정이 존재하는 경우:

- 같은 계정으로 자동 연동 (이메일 기준 병합) 권장
- 구글은 이메일 인증이 완료된 계정만 제공하므로 연동 시 이메일 인증 처리 별도 불필요
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
| 구글 인증 취소 | `구글 로그인을 취소했습니다.` |
| 이메일 미제공 | `구글 계정의 이메일 정보를 가져올 수 없습니다.` |
| 관리자 계정 없음 | `가입되지 않은 구글 계정입니다.` |
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

## 8. 구글 특이사항

### 8-1. 이메일 인증 여부

구글 OAuth에서 받은 사용자 정보에는 `email_verified` 필드가 있습니다. 이메일이 인증되지 않은 경우 처리 방법:

```java
// OAuth2User에서 email_verified 확인
Boolean emailVerified = oAuth2User.getAttribute("email_verified");
if (Boolean.FALSE.equals(emailVerified)) {
    // 미인증 이메일 처리: 에러 리다이렉트
    redirectToErrorPage("구글 이메일 인증이 필요합니다.");
    return;
}
```

### 8-2. 구글 사용자 정보 응답 구조

구글 User Info API 응답 (플랫 구조):

```json
{
  "sub": "1234567890",
  "name": "홍길동",
  "given_name": "길동",
  "family_name": "홍",
  "picture": "https://lh3.googleusercontent.com/...",
  "email": "user@gmail.com",
  "email_verified": true,
  "locale": "ko"
}
```

다른 제공자(카카오, 네이버)와 달리 중첩 구조가 없어 속성 접근이 단순합니다.

### 8-3. 개발 환경에서의 HTTPS

구글 OAuth는 프로덕션 환경에서 HTTPS를 요구합니다. 로컬 개발 시 `http://localhost:8080`은 허용되지만, 스테이징/운영에서는 반드시 HTTPS URI를 등록해야 합니다.

### 8-4. OAuth 동의 화면 설정

Google Cloud Console에서 "OAuth 동의 화면"을 반드시 설정해야 합니다:

- 앱 이름: `YEMO`
- 사용자 지원 이메일: 관리자 이메일
- 앱 도메인: 운영 도메인
- 개발자 연락처 이메일: 개발자 이메일

기본 scope(`email`, `profile`, `openid`)만 사용하는 경우 Google 심사 없이 바로 사용 가능합니다.

---

## 9. 프론트엔드 연동 포인트 요약

| 항목 | 위치 |
|------|------|
| 구글 로그인 버튼 | `src/pages/login.vue` - `handleGoogleLogin()` |
| OAuth 콜백 수신 | `src/pages/oauth2-redirect.vue` - `handleAdminLogin()` |
| 토큰 저장 | `localStorage.accessToken`, `localStorage.refreshToken` |
| 사용자 정보 조회 | `GET /api/auth/me` (로그인 후 자동 호출) |
| 로그인 후 이동 | `/shop-admin/dashboard` |
| 에러 시 이동 | `/login` |
| OAUTH_BASE_URL 계산 | `VITE_API_BASE_URL`에서 `/api` 제거한 값 |

---

## 10. 세 가지 OAuth 제공자 통합 비교

| 항목 | 카카오 | 네이버 | 구글 |
|------|--------|--------|------|
| 개발자 콘솔 | developers.kakao.com | developers.naver.com | console.cloud.google.com |
| Spring Security Built-in | 아니오 (직접 설정) | 아니오 (직접 설정) | 예 (기본 제공) |
| 사용자 정보 구조 | 중첩 (kakao_account.profile) | 중첩 (response) | 플랫 |
| 이메일 scope | `account_email` | `email` | `email` (기본) |
| 이름 scope | `profile_nickname` | `name` | `profile` (기본) |
| 이메일 인증 확인 | 별도 처리 불필요 | 별도 처리 불필요 | `email_verified` 필드 확인 |
| state 파라미터 | 선택 | 필수 | 선택 |
| 배포 심사 | 서비스 배포 시 검수 | 서비스 적용 신청 | 기본 scope는 심사 불필요 |
| HTTPS 요구 | 운영 환경 필요 | 운영 환경 필요 | 운영 환경 필요 |

---

## 11. 공통 구현 권장사항

세 가지 OAuth 제공자(카카오, 네이버, 구글)를 동시에 구현할 경우, 공통 처리를 위한 추상화를 권장합니다:

```java
// CustomOAuth2UserService - 세 제공자 통합 처리
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        // 제공자별 이메일/이름 추출
        OAuthAttributes attributes = OAuthAttributes.of(registrationId, oAuth2User.getAttributes());

        // DB에서 사용자 조회 또는 신규 생성
        User user = saveOrUpdate(attributes, registrationId);

        return new DefaultOAuth2User(
            Collections.singleton(new SimpleGrantedAuthority(user.getRole().name())),
            attributes.getAttributes(),
            attributes.getNameAttributeKey()
        );
    }
}

// OAuthAttributes - 제공자별 추출 로직
public class OAuthAttributes {
    public static OAuthAttributes of(String registrationId, Map<String, Object> attributes) {
        return switch (registrationId) {
            case "kakao"  -> ofKakao(attributes);
            case "naver"  -> ofNaver(attributes);
            case "google" -> ofGoogle(attributes);
            default -> throw new IllegalArgumentException("Unknown registrationId: " + registrationId);
        };
    }
}
```
