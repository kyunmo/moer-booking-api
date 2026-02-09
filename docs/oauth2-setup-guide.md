# OAuth2 SNS 로그인 설정 가이드

이 문서는 moer 예약 시스템에서 구글, 네이버, 카카오 SNS 로그인을 설정하는 방법을 설명합니다.

---

## 📋 목차

1. [구글 OAuth2 설정](#1-구글-oauth2-설정)
2. [네이버 OAuth2 설정](#2-네이버-oauth2-설정)
3. [카카오 OAuth2 설정](#3-카카오-oauth2-설정)
4. [환경 변수 설정](#4-환경-변수-설정)
5. [테스트 방법](#5-테스트-방법)

---

## 1. 구글 OAuth2 설정

### 1.1 Google Cloud Console 접속

1. https://console.cloud.google.com/ 접속
2. Google 계정으로 로그인

### 1.2 프로젝트 생성

1. 상단 메뉴에서 **프로젝트 선택** → **새 프로젝트**
2. 프로젝트 이름 입력 (예: `moer-booking`)
3. **만들기** 클릭

### 1.3 OAuth 동의 화면 구성

1. 좌측 메뉴: **API 및 서비스** → **OAuth 동의 화면**
2. User Type: **외부(External)** 선택 → **만들기**
3. 앱 정보 입력:
   - 앱 이름: `moer 예약 시스템`
   - 사용자 지원 이메일: 본인 이메일
   - 개발자 연락처: 본인 이메일
4. **저장 후 계속** 클릭
5. 범위 추가:
   - **범위 추가 또는 삭제** 클릭
   - `.../auth/userinfo.email` 체크
   - `.../auth/userinfo.profile` 체크
   - **업데이트** 클릭
6. **저장 후 계속** 클릭
7. 테스트 사용자 추가 (선택사항)
8. **대시보드로 돌아가기** 클릭

### 1.4 OAuth 2.0 클라이언트 ID 만들기

1. 좌측 메뉴: **API 및 서비스** → **사용자 인증 정보**
2. **+ 사용자 인증 정보 만들기** → **OAuth 2.0 클라이언트 ID**
3. 애플리케이션 유형: **웹 애플리케이션**
4. 이름 입력 (예: `moer-web-client`)
5. **승인된 리디렉션 URI** 추가:
   ```
   http://localhost:8080/login/oauth2/code/google
   ```
   - 프로덕션 배포 시:
     ```
     https://yourdomain.com/login/oauth2/code/google
     ```
6. **만들기** 클릭
7. **클라이언트 ID**와 **클라이언트 보안 비밀번호** 복사하여 안전하게 보관

325667166855-l1er4hseic3pn5dfmqs8cpv7kq79hp0k.apps.googleusercontent.com
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


### 1.5 환경 변수 설정

```bash
GOOGLE_CLIENT_ID=your-google-client-id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=GOCSPX-xxxxxxxxxxxxxxxxxxxxx
```

---

## 2. 네이버 OAuth2 설정

### 2.1 네이버 개발자 센터 접속

1. https://developers.naver.com/apps/ 접속
2. 네이버 계정으로 로그인

### 2.2 애플리케이션 등록

1. **애플리케이션 등록** 버튼 클릭
2. 애플리케이션 정보 입력:
   - **애플리케이션 이름**: `moer 예약 시스템`
   - **사용 API**: **네이버 로그인** 체크
   - **제공 정보 선택**:
     - 회원이름 (필수)
     - 이메일 주소 (필수)
     - 프로필 사진 (선택)

### 2.3 환경 추가 및 Callback URL 설정

1. **서비스 환경** 선택:
   - **PC 웹** 체크
2. **서비스 URL** 입력:
   ```
   http://localhost:8080
   ```
   - 프로덕션:
     ```
     https://yourdomain.com
     ```
3. **Callback URL** 입력:
   ```
   http://localhost:8080/login/oauth2/code/naver
   ```
   - 프로덕션:
     ```
     https://yourdomain.com/login/oauth2/code/naver
     ```
4. **등록하기** 클릭

### 2.4 Client ID 및 Secret 확인

1. 등록한 애플리케이션 클릭
2. **Client ID** 복사
3. **Client Secret** 생성 → 복사

### 2.5 환경 변수 설정

```bash
NAVER_CLIENT_ID=m5JkQiqh_8UyKO365EAh
NAVER_CLIENT_SECRET=JcUZkcUPhe
```

---

## 3. 카카오 OAuth2 설정

### 3.1 카카오 개발자 센터 접속

1. https://developers.kakao.com/ 접속
2. 카카오 계정으로 로그인

### 3.2 애플리케이션 추가

1. **내 애플리케이션** 메뉴 클릭
2. **애플리케이션 추가하기** 클릭
3. 앱 정보 입력:
   - **앱 이름**: `moer 예약 시스템`
   - **사업자명**: 본인 이름 또는 회사명
4. **저장** 클릭

### 3.3 플랫폼 설정

1. 생성한 앱 선택
2. 좌측 메뉴: **앱 설정** → **플랫폼**
3. **Web 플랫폼 등록** 클릭
4. **사이트 도메인** 입력:
   ```
   http://localhost:8080
   ```
   - 프로덕션:
     ```
     https://yourdomain.com
     ```
5. **저장** 클릭

### 3.4 Redirect URI 설정

1. 좌측 메뉴: **제품 설정** → **카카오 로그인**
2. **활성화 설정** → **ON**
3. **Redirect URI 등록** 클릭
4. Redirect URI 입력:
   ```
   http://localhost:8080/login/oauth2/code/kakao
   ```
   - 프로덕션:
     ```
     https://yourdomain.com/login/oauth2/code/kakao
     ```
5. **저장** 클릭

### 3.5 동의 항목 설정

1. 좌측 메뉴: **제품 설정** → **카카오 로그인** → **동의 항목**
2. 다음 항목 설정:
   - **프로필 정보 (닉네임/프로필 사진)**:
     - 설정: **필수 동의**
   - **카카오계정(이메일)**:
     - 설정: **필수 동의**
     - **이메일** 체크
3. **저장** 클릭

### 3.6 보안 설정 (Client Secret 활성화)

1. 좌측 메뉴: **제품 설정** → **카카오 로그인** → **보안**
2. **Client Secret** → **코드 생성** 클릭
3. 생성된 코드 복사
4. **활성화 상태** → **사용함** 선택

### 3.7 앱 키 확인

1. 좌측 메뉴: **앱 설정** → **앱 키**
2. **REST API 키** 복사 (Client ID로 사용)
3. **Client Secret** 복사 (보안 탭에서 생성한 것)

### 3.8 환경 변수 설정

```bash
KAKAO_CLIENT_ID=37ec5f253dda4a9f0c543ee0249557f5
KAKAO_CLIENT_SECRET=your_kakao_client_secret
```

---

## 4. 환경 변수 설정

### 4.1 개발 환경 (.env 파일 또는 IDE 설정)

```bash
# Google OAuth2
GOOGLE_CLIENT_ID=your-google-client-id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=GOCSPX-xxxxxxxxxxxxxxxxxxxxx

# Naver OAuth2
NAVER_CLIENT_ID=your_naver_client_id
NAVER_CLIENT_SECRET=your_naver_client_secret

# Kakao OAuth2
KAKAO_CLIENT_ID=your_kakao_rest_api_key
KAKAO_CLIENT_SECRET=your_kakao_client_secret

# OAuth2 Redirect URI (프론트엔드)
OAUTH2_REDIRECT_URI=http://localhost:3000/oauth2/redirect
```

### 4.2 프로덕션 환경 (Docker, AWS, 등)

```bash
# Google OAuth2
export GOOGLE_CLIENT_ID="production-google-client-id"
export GOOGLE_CLIENT_SECRET="production-google-client-secret"

# Naver OAuth2
export NAVER_CLIENT_ID="production-naver-client-id"
export NAVER_CLIENT_SECRET="production-naver-client-secret"

# Kakao OAuth2
export KAKAO_CLIENT_ID="production-kakao-client-id"
export KAKAO_CLIENT_SECRET="production-kakao-client-secret"

# OAuth2 Redirect URI
export OAUTH2_REDIRECT_URI="https://yourdomain.com/oauth2/redirect"
```

### 4.3 IntelliJ IDEA 설정

1. **Run** → **Edit Configurations**
2. **Environment variables** 클릭
3. 위의 환경 변수들을 입력
4. **OK** 클릭

### 4.4 application.yml 확인

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
          naver:
            client-id: ${NAVER_CLIENT_ID}
            client-secret: ${NAVER_CLIENT_SECRET}
          kakao:
            client-id: ${KAKAO_CLIENT_ID}
            client-secret: ${KAKAO_CLIENT_SECRET}

app:
  oauth2:
    redirect-uri: ${OAUTH2_REDIRECT_URI:http://localhost:3000/oauth2/redirect}
```

---

## 5. 테스트 방법

### 5.1 로컬 테스트

1. 애플리케이션 실행:
   ```bash
   ./gradlew bootRun
   ```

2. 브라우저에서 접속:
   - 구글: http://localhost:8080/oauth2/authorization/google
   - 네이버: http://localhost:8080/oauth2/authorization/naver
   - 카카오: http://localhost:8080/oauth2/authorization/kakao

3. 각 SNS 로그인 화면에서 로그인

4. 성공 시 프론트엔드 리다이렉트 URL로 이동:
   ```
   http://localhost:3000/oauth2/redirect?accessToken=xxx&refreshToken=yyy
   ```

### 5.2 Swagger UI 테스트

현재 OAuth2는 Swagger UI에서 직접 테스트하기 어렵습니다. 브라우저를 통해 테스트하세요.

### 5.3 프론트엔드 통합

프론트엔드에서 다음과 같이 사용:

```javascript
// 구글 로그인 버튼 클릭 시
window.location.href = 'http://localhost:8080/oauth2/authorization/google';

// 네이버 로그인 버튼 클릭 시
window.location.href = 'http://localhost:8080/oauth2/authorization/naver';

// 카카오 로그인 버튼 클릭 시
window.location.href = 'http://localhost:8080/oauth2/authorization/kakao';

// OAuth2 Redirect 페이지에서 토큰 추출
const urlParams = new URLSearchParams(window.location.search);
const accessToken = urlParams.get('accessToken');
const refreshToken = urlParams.get('refreshToken');

// 토큰을 localStorage에 저장
localStorage.setItem('accessToken', accessToken);
localStorage.setItem('refreshToken', refreshToken);

// 메인 페이지로 이동
window.location.href = '/dashboard';
```

---

## 6. 문제 해결

### 6.1 리다이렉트 URI 불일치

**증상**: `redirect_uri_mismatch` 오류

**해결**:
1. OAuth2 제공자의 콘솔에서 등록한 Redirect URI 확인
2. application.yml의 `redirect-uri` 설정 확인
3. 대소문자, 슬래시(/) 포함 여부까지 정확히 일치해야 함

### 6.2 이메일 정보 미제공

**증상**: 사용자가 이메일 제공에 동의하지 않음

**해결**:
1. OAuth2 동의 화면에서 이메일을 필수로 설정
2. 카카오의 경우 비즈니스 앱으로 전환 필요

### 6.3 토큰 만료

**증상**: `invalid_token` 오류

**해결**:
1. Access Token은 1시간마다 갱신 필요
2. Refresh Token 사용하여 새 Access Token 발급
3. `/api/auth/refresh` 엔드포인트 사용

### 6.4 CORS 오류

**증상**: 프론트엔드에서 `CORS policy` 오류

**해결**:
1. `SecurityConfig.java`의 CORS 설정 확인
2. 프론트엔드 도메인이 허용 목록에 포함되어 있는지 확인

---

## 7. 보안 주의사항

### 7.1 Client Secret 관리

- ⚠️ **절대 Git에 커밋하지 마세요**
- `.gitignore`에 `.env` 파일 추가
- 환경 변수로만 관리
- 프로덕션과 개발 환경의 키를 분리

### 7.2 Redirect URI 제한

- 프로덕션에서는 HTTPS 사용 필수
- localhost는 개발 환경에서만 사용
- 정확한 URI만 등록 (와일드카드 사용 금지)

### 7.3 토큰 저장

- Access Token과 Refresh Token은 안전하게 저장
- 프론트엔드에서는 httpOnly 쿠키 사용 권장
- localStorage는 XSS 공격에 취약

---

## 8. 추가 자료

- [Spring Security OAuth2 공식 문서](https://docs.spring.io/spring-security/reference/servlet/oauth2/login/core.html)
- [Google OAuth2 문서](https://developers.google.com/identity/protocols/oauth2)
- [네이버 로그인 API](https://developers.naver.com/docs/login/api/)
- [카카오 로그인 API](https://developers.kakao.com/docs/latest/ko/kakaologin/rest-api)

---

## 9. 문의

기술 지원이 필요하면 다음 방법으로 문의하세요:

- GitHub Issues: https://github.com/your-repo/moer-booking/issues
- 이메일: support@moer-booking.com
