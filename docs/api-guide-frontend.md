# moer 예약 시스템 - 프론트엔드 API 가이드

## 📋 목차

1. [회원가입 & 로그인](#1-회원가입--로그인)
2. [30일 체험판](#2-30일-체험판)
3. [비밀번호 찾기](#3-비밀번호-찾기)
4. [SNS 로그인](#4-sns-로그인)
5. [에러 처리](#5-에러-처리)
6. [인증 토큰 관리](#6-인증-토큰-관리)

---

## 기본 정보

### Base URL
```
개발: http://localhost:8080
프로덕션: https://api.moer-booking.com
```

### 공통 응답 형식
```json
{
  "success": true,
  "data": { ... },
  "message": "선택적 메시지",
  "timestamp": "2024-02-09T21:00:00"
}
```

### 인증 헤더
```http
Authorization: Bearer {accessToken}
```

---

## 1. 회원가입 & 로그인

### 1.1 회원가입

**[POST] `/api/auth/register`**

신규 사용자 및 매장을 동시에 생성하고, 자동으로 30일 체험판이 활성화됩니다.

**Request Body:**
```json
{
  "email": "owner@example.com",
  "password": "password123",
  "name": "홍길동",
  "phone": "010-1234-5678",
  "businessName": "홍길동 헤어샵",
  "businessType": "BEAUTY_SHOP"
}
```

**businessType 옵션:**
- `BEAUTY_SHOP`: 미용실
- `PILATES`: 필라테스
- `YOGA`: 요가
- `CAFE`: 카페
- `RESTAURANT`: 레스토랑
- `OTHER`: 기타

**Response (201 Created):**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
      "id": 1,
      "email": "owner@example.com",
      "name": "홍길동",
      "phone": "010-1234-5678",
      "role": "OWNER",
      "status": "ACTIVE",
      "emailVerified": "N",
      "businessId": 1
    },
    "business": {
      "id": 1,
      "name": "홍길동 헤어샵",
      "businessType": "BEAUTY_SHOP",
      "status": "ACTIVE",
      "ownerId": 1
    },
    "trial": {
      "startedAt": "2024-02-09T21:00:00",
      "expiresAt": "2024-03-10T21:00:00",
      "remainingDays": 30,
      "isPremium": false
    }
  },
  "timestamp": "2024-02-09T21:00:00"
}
```

### 1.2 로그인

**[POST] `/api/auth/login`**

**Request Body:**
```json
{
  "email": "owner@example.com",
  "password": "password123"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
      "id": 1,
      "email": "owner@example.com",
      "name": "홍길동",
      "role": "OWNER",
      "businessId": 1
    }
  }
}
```

### 1.3 토큰 갱신

**[POST] `/api/auth/refresh`**

Access Token이 만료되면 Refresh Token으로 새 토큰을 발급받습니다.

**Request Body:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600
  }
}
```

### 1.4 로그아웃

**[POST] `/api/auth/logout`**

**Headers:**
```http
Authorization: Bearer {accessToken}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": null
}
```

### 1.5 현재 사용자 정보

**[GET] `/api/auth/me`**

**Headers:**
```http
Authorization: Bearer {accessToken}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "email": "owner@example.com",
    "name": "홍길동",
    "phone": "010-1234-5678",
    "role": "OWNER",
    "status": "ACTIVE",
    "businessId": 1
  }
}
```

---

## 2. 30일 체험판

### 2.1 개요

- 회원가입 시 자동으로 30일 체험판이 활성화됩니다.
- 체험판 정보는 회원가입 응답에 포함됩니다.
- 대시보드 API에서 현재 체험판 상태를 확인할 수 있습니다.

### 2.2 대시보드에서 체험판 정보 확인

**[GET] `/api/businesses/{businessId}/dashboard`**

**Headers:**
```http
Authorization: Bearer {accessToken}
```

**Query Parameters:**
- `date` (optional): 조회 날짜 (기본: 오늘, 형식: YYYY-MM-DD)

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "todayStats": { ... },
    "weekStats": { ... },
    "monthStats": { ... },
    "trialProgress": {
      "startedAt": "2024-02-09T21:00:00",
      "expiresAt": "2024-03-10T21:00:00",
      "totalDays": 30,
      "remainingDays": 25,
      "usedDays": 5,
      "isPremium": false,
      "isExpired": false
    }
  }
}
```

### 2.3 프론트엔드 구현 가이드

#### 체험판 진행 상황 표시

```jsx
// 체험판 진행 바 컴포넌트
function TrialProgressBar({ trialProgress }) {
  if (trialProgress.isPremium) {
    return <div>프리미엄 사용자입니다 🎉</div>;
  }

  const percentage = (trialProgress.usedDays / trialProgress.totalDays) * 100;

  return (
    <div className="trial-progress">
      <div className="progress-bar">
        <div className="progress-fill" style={{ width: `${percentage}%` }} />
      </div>
      <p>
        체험판 {trialProgress.remainingDays}일 남음
        (만료일: {new Date(trialProgress.expiresAt).toLocaleDateString()})
      </p>
      {trialProgress.remainingDays <= 7 && (
        <button className="upgrade-btn">프리미엄으로 업그레이드</button>
      )}
    </div>
  );
}
```

#### 만료 알림

```jsx
function TrialExpiryAlert({ trialProgress }) {
  if (trialProgress.isPremium || !trialProgress.isExpired) {
    return null;
  }

  return (
    <div className="alert alert-danger">
      <h4>체험판이 만료되었습니다</h4>
      <p>프리미엄으로 업그레이드하여 계속 사용하세요.</p>
      <button>지금 업그레이드</button>
    </div>
  );
}
```

---

## 3. 비밀번호 찾기

### 3.1 비밀번호 재설정 요청

**[POST] `/api/auth/forgot-password`**

등록된 이메일로 비밀번호 재설정 링크를 발송합니다.

**Request Body:**
```json
{
  "email": "owner@example.com"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": null,
  "message": "비밀번호 재설정 이메일을 발송했습니다. 이메일을 확인해주세요."
}
```

### 3.2 비밀번호 재설정 실행

**[POST] `/api/auth/reset-password`**

이메일에서 받은 토큰으로 새 비밀번호를 설정합니다.

**Request Body:**
```json
{
  "token": "550e8400-e29b-41d4-a716-446655440000",
  "newPassword": "newPassword123"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": null,
  "message": "비밀번호가 재설정되었습니다. 새 비밀번호로 로그인해주세요."
}
```

### 3.3 프론트엔드 구현 가이드

#### 비밀번호 찾기 화면

```jsx
function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      const response = await fetch('http://localhost:8080/api/auth/forgot-password', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email })
      });

      const data = await response.json();

      if (data.success) {
        setSuccess(true);
        alert(data.message);
      } else {
        alert(data.error.message);
      }
    } catch (error) {
      alert('오류가 발생했습니다. 다시 시도해주세요.');
    } finally {
      setLoading(false);
    }
  };

  if (success) {
    return (
      <div>
        <h2>이메일을 확인하세요</h2>
        <p>비밀번호 재설정 링크를 {email}로 발송했습니다.</p>
        <p>이메일을 확인하고 링크를 클릭하여 비밀번호를 재설정하세요.</p>
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit}>
      <h2>비밀번호 찾기</h2>
      <input
        type="email"
        placeholder="이메일 주소"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        required
      />
      <button type="submit" disabled={loading}>
        {loading ? '발송 중...' : '재설정 링크 발송'}
      </button>
    </form>
  );
}
```

#### 비밀번호 재설정 화면

```jsx
function ResetPasswordPage() {
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  // URL에서 토큰 추출
  const searchParams = new URLSearchParams(window.location.search);
  const token = searchParams.get('token');

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (newPassword !== confirmPassword) {
      alert('비밀번호가 일치하지 않습니다.');
      return;
    }

    if (newPassword.length < 8) {
      alert('비밀번호는 최소 8자 이상이어야 합니다.');
      return;
    }

    setLoading(true);

    try {
      const response = await fetch('http://localhost:8080/api/auth/reset-password', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ token, newPassword })
      });

      const data = await response.json();

      if (data.success) {
        alert(data.message);
        navigate('/login');
      } else {
        alert(data.error.message);
      }
    } catch (error) {
      alert('오류가 발생했습니다. 다시 시도해주세요.');
    } finally {
      setLoading(false);
    }
  };

  if (!token) {
    return <div>유효하지 않은 링크입니다.</div>;
  }

  return (
    <form onSubmit={handleSubmit}>
      <h2>새 비밀번호 설정</h2>
      <input
        type="password"
        placeholder="새 비밀번호 (최소 8자)"
        value={newPassword}
        onChange={(e) => setNewPassword(e.target.value)}
        required
      />
      <input
        type="password"
        placeholder="비밀번호 확인"
        value={confirmPassword}
        onChange={(e) => setConfirmPassword(e.target.value)}
        required
      />
      <button type="submit" disabled={loading}>
        {loading ? '처리 중...' : '비밀번호 재설정'}
      </button>
    </form>
  );
}
```

### 3.4 이메일 템플릿 안내

사용자가 받는 이메일은 다음과 같은 형식입니다:

```
제목: [moer] 비밀번호 재설정 안내

안녕하세요, 홍길동님

비밀번호 재설정을 요청하셨습니다.

[비밀번호 재설정하기] 버튼 클릭

이 링크는 30분간만 유효합니다.
요청하지 않으셨다면 이 이메일을 무시하세요.
```

---

## 4. SNS 로그인

### 4.1 지원 SNS

- ✅ 구글 (Google)
- ✅ 네이버 (Naver)
- ✅ 카카오 (Kakao)

### 4.2 SNS 로그인 플로우

```
1. 사용자: SNS 로그인 버튼 클릭
   ↓
2. 프론트엔드: 백엔드 OAuth2 엔드포인트로 리다이렉트
   ↓
3. 사용자: SNS 로그인 페이지에서 인증
   ↓
4. 백엔드: 사용자 정보 처리 및 JWT 토큰 생성
   ↓
5. 백엔드: 프론트엔드로 리다이렉트 (토큰 포함)
   ↓
6. 프론트엔드: 토큰 저장 및 메인 페이지로 이동
```

### 4.3 SNS 로그인 시작

#### 구글 로그인
```
[GET] /oauth2/authorization/google
```

#### 네이버 로그인
```
[GET] /oauth2/authorization/naver
```

#### 카카오 로그인
```
[GET] /oauth2/authorization/kakao
```

**사용법:**
```javascript
// 구글 로그인 버튼 클릭 시
window.location.href = 'http://localhost:8080/oauth2/authorization/google';

// 네이버 로그인 버튼 클릭 시
window.location.href = 'http://localhost:8080/oauth2/authorization/naver';

// 카카오 로그인 버튼 클릭 시
window.location.href = 'http://localhost:8080/oauth2/authorization/kakao';
```

### 4.4 OAuth2 Redirect (콜백)

SNS 로그인 성공 후 다음 URL로 리다이렉트됩니다:

```
http://localhost:3000/oauth2/redirect?accessToken={token}&refreshToken={token}
```

실패 시:
```
http://localhost:3000/oauth2/redirect?error=oauth2_failed&message={errorMessage}
```

### 4.5 프론트엔드 구현 가이드

#### 로그인 화면 - SNS 버튼

```jsx
function LoginPage() {
  const handleGoogleLogin = () => {
    window.location.href = 'http://localhost:8080/oauth2/authorization/google';
  };

  const handleNaverLogin = () => {
    window.location.href = 'http://localhost:8080/oauth2/authorization/naver';
  };

  const handleKakaoLogin = () => {
    window.location.href = 'http://localhost:8080/oauth2/authorization/kakao';
  };

  return (
    <div className="login-page">
      <h2>로그인</h2>

      {/* 일반 로그인 폼 */}
      <form onSubmit={handleEmailLogin}>
        <input type="email" placeholder="이메일" />
        <input type="password" placeholder="비밀번호" />
        <button type="submit">로그인</button>
      </form>

      <div className="divider">또는</div>

      {/* SNS 로그인 버튼 */}
      <div className="sns-login-buttons">
        <button onClick={handleGoogleLogin} className="btn-google">
          <img src="/icons/google.svg" alt="Google" />
          구글로 로그인
        </button>

        <button onClick={handleNaverLogin} className="btn-naver">
          <img src="/icons/naver.svg" alt="Naver" />
          네이버로 로그인
        </button>

        <button onClick={handleKakaoLogin} className="btn-kakao">
          <img src="/icons/kakao.svg" alt="Kakao" />
          카카오로 로그인
        </button>
      </div>

      <div className="login-links">
        <a href="/forgot-password">비밀번호를 잊으셨나요?</a>
        <a href="/register">회원가입</a>
      </div>
    </div>
  );
}
```

#### OAuth2 Redirect 페이지

```jsx
// src/pages/OAuth2RedirectPage.jsx
import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

function OAuth2RedirectPage() {
  const navigate = useNavigate();

  useEffect(() => {
    // URL에서 쿼리 파라미터 추출
    const searchParams = new URLSearchParams(window.location.search);
    const accessToken = searchParams.get('accessToken');
    const refreshToken = searchParams.get('refreshToken');
    const error = searchParams.get('error');
    const errorMessage = searchParams.get('message');

    if (error) {
      // 로그인 실패
      console.error('OAuth2 로그인 실패:', errorMessage);
      alert(`로그인에 실패했습니다: ${errorMessage}`);
      navigate('/login');
      return;
    }

    if (accessToken && refreshToken) {
      // 로그인 성공
      // 토큰을 localStorage에 저장
      localStorage.setItem('accessToken', accessToken);
      localStorage.setItem('refreshToken', refreshToken);

      // 사용자 정보 가져오기
      fetchUserInfo(accessToken)
        .then(user => {
          localStorage.setItem('user', JSON.stringify(user));
          // 대시보드로 이동
          navigate('/dashboard');
        })
        .catch(err => {
          console.error('사용자 정보 가져오기 실패:', err);
          navigate('/login');
        });
    } else {
      // 토큰이 없는 경우
      alert('로그인 정보를 받지 못했습니다.');
      navigate('/login');
    }
  }, [navigate]);

  return (
    <div className="loading-page">
      <div className="spinner"></div>
      <p>로그인 처리 중...</p>
    </div>
  );
}

async function fetchUserInfo(accessToken) {
  const response = await fetch('http://localhost:8080/api/auth/me', {
    headers: {
      'Authorization': `Bearer ${accessToken}`
    }
  });

  if (!response.ok) {
    throw new Error('사용자 정보를 가져올 수 없습니다');
  }

  const data = await response.json();
  return data.data;
}

export default OAuth2RedirectPage;
```

#### 라우터 설정 (React Router)

```jsx
// src/App.jsx
import { BrowserRouter, Routes, Route } from 'react-router-dom';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/forgot-password" element={<ForgotPasswordPage />} />
        <Route path="/reset-password" element={<ResetPasswordPage />} />
        <Route path="/oauth2/redirect" element={<OAuth2RedirectPage />} />
        <Route path="/dashboard" element={<DashboardPage />} />
        {/* ... 기타 라우트 */}
      </Routes>
    </BrowserRouter>
  );
}
```

### 4.6 SNS 로그인 특징

1. **자동 계정 연동**
   - 같은 이메일로 기존 계정이 있으면 자동 연동
   - 여러 SNS를 하나의 계정에 연동 가능

2. **신규 사용자 생성**
   - SNS 로그인 시 자동으로 사용자 및 매장 생성
   - 30일 체험판 자동 활성화
   - 이메일 인증 완료 처리

3. **보안**
   - JWT 토큰 발급 (일반 로그인과 동일)
   - Access Token: 1시간
   - Refresh Token: 7일

---

## 5. 에러 처리

### 5.1 에러 응답 형식

```json
{
  "success": false,
  "error": {
    "code": "U001",
    "message": "사용자를 찾을 수 없습니다"
  },
  "timestamp": "2024-02-09T21:00:00"
}
```

### 5.2 주요 에러 코드

#### 공통 에러 (C001~C006)
| 코드 | HTTP | 메시지 | 설명 |
|------|------|--------|------|
| C001 | 400 | 잘못된 입력값입니다 | 유효성 검증 실패 |
| C005 | 404 | 엔티티를 찾을 수 없습니다 | 리소스 없음 |
| C006 | 403 | 접근 권한이 없습니다 | 권한 부족 |

#### 인증/권한 에러 (A001~A005)
| 코드 | HTTP | 메시지 | 설명 |
|------|------|--------|------|
| A001 | 401 | 인증이 필요합니다 | 토큰 없음 |
| A002 | 401 | 유효하지 않은 토큰입니다 | 토큰 검증 실패 |
| A003 | 401 | 만료된 토큰입니다 | 토큰 만료 |

#### 사용자 에러 (U001~U004)
| 코드 | HTTP | 메시지 | 설명 |
|------|------|--------|------|
| U001 | 404 | 사용자를 찾을 수 없습니다 | 존재하지 않는 사용자 |
| U002 | 409 | 이미 사용 중인 이메일입니다 | 이메일 중복 |
| U004 | 400 | 비밀번호가 일치하지 않습니다 | 로그인 실패 |

#### 체험판 에러 (TR001~TR003)
| 코드 | HTTP | 메시지 | 설명 |
|------|------|--------|------|
| TR001 | 403 | 체험판 기간이 만료되었습니다 | 체험판 만료 |
| TR002 | 403 | 체험판에서는 사용할 수 없는 기능입니다 | 기능 제한 |
| TR003 | 402 | 프리미엄 업그레이드가 필요합니다 | 업그레이드 필요 |

#### 비밀번호 재설정 에러 (PR001~PR003)
| 코드 | HTTP | 메시지 | 설명 |
|------|------|--------|------|
| PR001 | 400 | 유효하지 않은 재설정 토큰입니다 | 잘못된 토큰 |
| PR002 | 400 | 만료된 재설정 토큰입니다 | 토큰 만료 (30분) |
| PR003 | 400 | 이미 사용된 재설정 토큰입니다 | 토큰 재사용 |

#### OAuth2 에러 (OA001~OA003)
| 코드 | HTTP | 메시지 | 설명 |
|------|------|--------|------|
| OA001 | 400 | 지원하지 않는 SNS 제공자입니다 | 잘못된 provider |
| OA002 | 401 | SNS 로그인에 실패했습니다 | OAuth2 인증 실패 |
| OA003 | 400 | SNS에서 이메일 정보를 제공하지 않았습니다 | 이메일 미제공 |

### 5.3 프론트엔드 에러 처리 예시

```jsx
// API 호출 공통 함수
async function apiCall(url, options = {}) {
  const token = localStorage.getItem('accessToken');

  const response = await fetch(url, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(token && { 'Authorization': `Bearer ${token}` }),
      ...options.headers,
    },
  });

  const data = await response.json();

  if (!data.success) {
    // 에러 처리
    const error = data.error;

    // 토큰 만료 시 자동 갱신
    if (error.code === 'A003') {
      const newToken = await refreshAccessToken();
      if (newToken) {
        // 재시도
        return apiCall(url, options);
      } else {
        // 로그인 페이지로 이동
        window.location.href = '/login';
        return;
      }
    }

    // 체험판 만료
    if (error.code === 'TR001') {
      // 업그레이드 페이지로 이동
      window.location.href = '/upgrade';
      return;
    }

    // 일반 에러 표시
    throw new Error(error.message);
  }

  return data.data;
}

// 토큰 갱신 함수
async function refreshAccessToken() {
  const refreshToken = localStorage.getItem('refreshToken');
  if (!refreshToken) {
    return null;
  }

  try {
    const response = await fetch('http://localhost:8080/api/auth/refresh', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken })
    });

    const data = await response.json();

    if (data.success) {
      localStorage.setItem('accessToken', data.data.accessToken);
      return data.data.accessToken;
    }
  } catch (error) {
    console.error('토큰 갱신 실패:', error);
  }

  return null;
}

// 사용 예시
try {
  const user = await apiCall('http://localhost:8080/api/auth/me');
  console.log('사용자 정보:', user);
} catch (error) {
  alert(error.message);
}
```

---

## 6. 인증 토큰 관리

### 6.1 토큰 저장

```javascript
// 로그인 성공 시
function saveTokens(accessToken, refreshToken) {
  localStorage.setItem('accessToken', accessToken);
  localStorage.setItem('refreshToken', refreshToken);
}

// 로그아웃 시
function clearTokens() {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
  localStorage.removeItem('user');
}
```

### 6.2 토큰 자동 갱신

```javascript
// Axios 인터셉터 예시
import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080/api',
});

// 요청 인터셉터: Access Token 자동 추가
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// 응답 인터셉터: 토큰 만료 시 자동 갱신
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    // 토큰 만료 에러 (A003)
    if (
      error.response?.status === 401 &&
      error.response?.data?.error?.code === 'A003' &&
      !originalRequest._retry
    ) {
      originalRequest._retry = true;

      try {
        // Refresh Token으로 새 Access Token 발급
        const refreshToken = localStorage.getItem('refreshToken');
        const response = await axios.post(
          'http://localhost:8080/api/auth/refresh',
          { refreshToken }
        );

        const newAccessToken = response.data.data.accessToken;
        localStorage.setItem('accessToken', newAccessToken);

        // 원래 요청 재시도
        originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
        return api(originalRequest);
      } catch (refreshError) {
        // Refresh Token도 만료된 경우
        clearTokens();
        window.location.href = '/login';
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);

export default api;
```

### 6.3 보호된 라우트 (Private Route)

```jsx
// src/components/PrivateRoute.jsx
import { Navigate } from 'react-router-dom';

function PrivateRoute({ children }) {
  const accessToken = localStorage.getItem('accessToken');

  if (!accessToken) {
    // 로그인하지 않은 경우
    return <Navigate to="/login" replace />;
  }

  return children;
}

// 사용 예시
<Route
  path="/dashboard"
  element={
    <PrivateRoute>
      <DashboardPage />
    </PrivateRoute>
  }
/>
```

---

## 7. 추가 참고 사항

### 7.1 CORS 설정

백엔드에서 다음 도메인에 대해 CORS를 허용합니다:
- `http://localhost:3000` (React 기본 포트)
- `http://localhost:5173` (Vite 기본 포트)

프로덕션 배포 시 실제 도메인을 백엔드 CORS 설정에 추가해야 합니다.

### 7.2 환경 변수 (.env 파일)

```bash
# .env.development
REACT_APP_API_URL=http://localhost:8080
REACT_APP_OAUTH2_REDIRECT_URI=http://localhost:3000/oauth2/redirect

# .env.production
REACT_APP_API_URL=https://api.moer-booking.com
REACT_APP_OAUTH2_REDIRECT_URI=https://app.moer-booking.com/oauth2/redirect
```

```javascript
// 사용 예시
const API_URL = process.env.REACT_APP_API_URL;

function loginWithGoogle() {
  window.location.href = `${API_URL}/oauth2/authorization/google`;
}
```

### 7.3 테스트 계정

개발 환경에서 테스트용으로 사용할 수 있는 계정:

```
이메일: test@example.com
비밀번호: test1234
```

### 7.4 Swagger UI

백엔드 API 문서를 확인하려면:
```
http://localhost:8080/swagger-ui.html
```

---

## 8. 문의 및 지원

- **기술 지원**: dev@moer-booking.com
- **버그 리포트**: GitHub Issues
- **문서 업데이트**: 2024-02-09

---

## 9. 변경 이력

### v1.0.0 (2024-02-09)
- ✅ 회원가입 & 로그인
- ✅ 30일 체험판 시스템
- ✅ 비밀번호 찾기
- ✅ SNS 로그인 (구글/네이버/카카오)
- ✅ JWT 토큰 인증
- ✅ 토큰 자동 갱신

---

**끝**
