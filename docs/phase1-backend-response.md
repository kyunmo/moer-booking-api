# Phase 1: 백엔드 API 응답서 — 프론트엔드 연동 가이드

> 작성일: 2026-02-14
> 대상: 프론트엔드 개발팀
> 참조: `docs/phase1-backend-request.md`

---

## 구현 완료 요약

| # | 요청 항목 | 구현 상태 | 비고 |
|---|----------|----------|------|
| 1-1 | 프로필 정보 수정 | **완료** | `PATCH /api/auth/profile` |
| 1-2 | 프로필 이미지 업로드 | **완료** | `POST /api/auth/profile/image` |
| 1-3 | 비밀번호 변경 | **완료** (신규 구현) | `POST /api/auth/change-password` |
| 1-4 | 회원 탈퇴 | **완료** | `DELETE /api/auth/account` |
| 1-5 | SNS 연결 계정 목록 | **완료** | `GET /api/auth/social-accounts` |
| 1-6 | SNS 계정 연결 해제 | **완료** | `DELETE /api/auth/social-accounts/{provider}` |
| 2 | OAuth/SNS 로그인 | **기존 완료** | Google/Kakao/Naver 설정 완료 |
| 3 | 비밀번호 찾기 이메일 | **기존 완료** | Gmail SMTP 설정 완료 |
| 4-B | 알림 시스템 (권장안) | **완료** | REST polling + 예약 이벤트 자동 알림 |

---

## 공통 응답 구조

### ApiResponse\<T\>

모든 API는 아래 형식으로 응답합니다.

```json
// 성공 (데이터 있음)
{
  "success": true,
  "data": { ... },
  "timestamp": "2026-02-14T10:30:00"
}

// 성공 (데이터 + 메시지)
{
  "success": true,
  "data": null,
  "message": "비밀번호가 변경되었습니다.",
  "timestamp": "2026-02-14T10:30:00"
}

// 실패
{
  "success": false,
  "error": {
    "code": "AC001",
    "message": "현재 비밀번호가 일치하지 않습니다"
  },
  "timestamp": "2026-02-14T10:30:00"
}
```

> `null` 값 필드는 JSON에 포함되지 않습니다 (`@JsonInclude(NON_NULL)`)

---

## 1. 프로필 정보 수정

### `PATCH /api/auth/profile`

현재 로그인한 사용자의 이름, 전화번호를 수정합니다. `null`인 필드는 변경하지 않습니다.

**인증**: Bearer Token 필요

#### Request Body

```json
{
  "name": "홍길동",          // 선택, 2~50자
  "phone": "010-1234-5678"  // 선택, 형식: XX-XXXX-XXXX 또는 XXX-XXXX-XXXX
}
```

| 필드 | 타입 | 필수 | 유효성 검증 |
|------|------|------|------------|
| `name` | String | 선택 | 2~50자 |
| `phone` | String | 선택 | 정규식: `^\d{2,3}-\d{3,4}-\d{4}$` |

#### Response 200

```json
{
  "success": true,
  "data": {
    "id": 1,
    "email": "user@example.com",
    "name": "홍길동",
    "phone": "010-1234-5678",
    "profileImageUrl": "/uploads/profiles/abc123.jpg",
    "role": "OWNER",
    "roleDescription": "매장 대표",
    "status": "ACTIVE",
    "statusDescription": "활성",
    "staffId": null,
    "businessId": 1,
    "emailVerified": true,
    "lastLoginAt": "2026-02-14 09:00:00",
    "createdAt": "2026-01-15 10:00:00",
    "updatedAt": "2026-02-14 10:30:00"
  },
  "timestamp": "2026-02-14T10:30:00"
}
```

#### Error Cases

| 에러코드 | HTTP | 설명 |
|---------|------|------|
| `C001` | 400 | 유효하지 않은 입력값 (이름 길이, 전화번호 형식 등) |
| `U001` | 404 | 사용자를 찾을 수 없음 |

---

## 2. 프로필 이미지 업로드

### `POST /api/auth/profile/image`

프로필 이미지를 업로드합니다. 기존 이미지가 있으면 자동으로 삭제 후 교체됩니다.

**인증**: Bearer Token 필요
**Content-Type**: `multipart/form-data`

#### Request

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `file` | MultipartFile | 필수 | 이미지 파일 |

**제약 조건**:
- 최대 파일 크기: **5MB**
- 허용 확장자: `jpg`, `jpeg`, `png`, `webp`
- 파일명은 UUID로 자동 변환됩니다

#### Response 200

```json
{
  "success": true,
  "data": {
    "profileImageUrl": "/uploads/profiles/a1b2c3d4-e5f6-7890-abcd-ef1234567890.jpg"
  },
  "timestamp": "2026-02-14T10:30:00"
}
```

#### Error Cases

| 에러코드 | HTTP | 설명 |
|---------|------|------|
| `FI001` | 500 | 파일 업로드 실패 (서버 오류) |
| `FI002` | 400 | 파일 크기 초과 (5MB) |
| `FI003` | 400 | 지원하지 않는 파일 형식 |
| `U001` | 404 | 사용자를 찾을 수 없음 |

#### 프론트엔드 연동 참고

```javascript
// FormData로 전송
const formData = new FormData()
formData.append('file', selectedFile)

const response = await axios.post('/api/auth/profile/image', formData, {
  headers: { 'Content-Type': 'multipart/form-data' }
})

// 이미지 URL 사용
const imageUrl = response.data.data.profileImageUrl
// → <img :src="imageUrl" /> 으로 표시 가능
// 정적 리소스로 /uploads/** 경로가 허용되어 있음
```

---

## 3. 비밀번호 변경

### `POST /api/auth/change-password`

현재 비밀번호를 확인하고 새 비밀번호로 변경합니다.
변경 후 **모든 세션의 Refresh Token이 삭제**됩니다 (재로그인 강제).

**인증**: Bearer Token 필요

#### Request Body

```json
{
  "currentPassword": "oldPw123",
  "newPassword": "newPw456",
  "confirmPassword": "newPw456"
}
```

| 필드 | 타입 | 필수 | 유효성 검증 |
|------|------|------|------------|
| `currentPassword` | String | 필수 | 빈 값 불가 |
| `newPassword` | String | 필수 | 8자 이상, 영문+숫자 포함 |
| `confirmPassword` | String | 필수 | `newPassword`와 일치해야 함 |

#### Response 200

```json
{
  "success": true,
  "message": "비밀번호가 변경되었습니다.",
  "timestamp": "2026-02-14T10:30:00"
}
```

#### Error Cases

| 에러코드 | HTTP | 설명 |
|---------|------|------|
| `AC001` | 400 | 현재 비밀번호 불일치 |
| `AC002` | 400 | 새 비밀번호와 확인 비밀번호 불일치 |
| `AC008` | 400 | 새 비밀번호가 현재 비밀번호와 동일 |
| `C001` | 400 | 비밀번호 정책 미충족 (8자 미만, 영문+숫자 미포함) |

#### 프론트엔드 주의사항

비밀번호 변경 성공 시, Refresh Token이 무효화되므로 **로그인 페이지로 리다이렉트** 처리가 필요합니다.

```javascript
try {
  await authApi.changePassword(data)
  // 로컬 토큰 삭제 후 로그인 페이지 이동
  localStorage.removeItem('accessToken')
  localStorage.removeItem('refreshToken')
  router.push('/login')
} catch (error) {
  // 에러 처리
}
```

---

## 4. 회원 탈퇴

### `DELETE /api/auth/account`

비밀번호를 확인하고 회원 탈퇴를 진행합니다. **Soft Delete** 방식으로 처리됩니다.

**인증**: Bearer Token 필요

#### Request Body

```json
{
  "password": "currentPw",        // 필수
  "reason": "서비스 불만족"         // 선택, 탈퇴 사유
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `password` | String | 필수 | 현재 비밀번호 확인용 |
| `reason` | String | 선택 | 탈퇴 사유 (감사 로그에 기록) |

#### Response 200

```json
{
  "success": true,
  "message": "회원 탈퇴가 완료되었습니다.",
  "timestamp": "2026-02-14T10:30:00"
}
```

#### 탈퇴 시 처리 내역 (Side Effects)

| 역할 | 처리 내용 |
|------|----------|
| **OWNER** | 소유 매장 비활성화, 매장 소속 전 직원 비활성화 |
| **STAFF** | 해당 직원 레코드만 비활성화 |
| **공통** | 사용자 상태 → `DELETED`, 모든 Refresh Token 삭제, SNS 계정 연결 해제 |

#### Error Cases

| 에러코드 | HTTP | 설명 |
|---------|------|------|
| `AC001` | 400 | 비밀번호 불일치 |
| `AC004` | 400 | 진행 중인 예약 존재 (PENDING/CONFIRMED 상태) |
| `AC005` | 400 | 이미 탈퇴된 계정 |
| `SA002` | 400 | 슈퍼 관리자는 탈퇴 불가 |

#### 프론트엔드 주의사항

- 탈퇴 전 **확인 다이얼로그** 표시 권장
- 진행 중인 예약이 있으면 서버에서 거부합니다 → 에러 메시지에 예약 건수가 포함됩니다
  - 예: `"진행 중인 예약 3건이 있습니다. 예약을 처리한 후 탈퇴해주세요."`
- 탈퇴 성공 후 **로컬 토큰 삭제 + 로그인 페이지 리다이렉트**
- 탈퇴된 계정으로 로그인 시도 시 `A004` 에러 발생 (메시지: "탈퇴된 계정입니다")

---

## 5. SNS 연결 계정 목록 조회

### `GET /api/auth/social-accounts`

현재 사용자에게 연결된 SNS 계정 목록을 조회합니다.

**인증**: Bearer Token 필요

#### Response 200

```json
{
  "success": true,
  "data": [
    {
      "provider": "GOOGLE",
      "email": "user@gmail.com",
      "name": "홍길동",
      "connectedAt": "2026-01-20 10:00:00"
    },
    {
      "provider": "KAKAO",
      "email": "user@kakao.com",
      "name": "길동",
      "connectedAt": "2026-01-22 14:00:00"
    }
  ],
  "timestamp": "2026-02-14T10:30:00"
}
```

#### 응답 필드

| 필드 | 타입 | 설명 |
|------|------|------|
| `provider` | String | `GOOGLE`, `KAKAO`, `NAVER` |
| `email` | String | SNS 계정 이메일 (null 가능) |
| `name` | String | SNS 계정 이름 (null 가능) |
| `connectedAt` | String | 연결 일시 (`yyyy-MM-dd HH:mm:ss`) |

> SNS 계정이 없으면 빈 배열 `[]`이 반환됩니다.

---

## 6. SNS 계정 연결 해제

### `DELETE /api/auth/social-accounts/{provider}`

특정 SNS 계정의 연결을 해제합니다.

**인증**: Bearer Token 필요

#### Path Parameters

| 파라미터 | 타입 | 필수 | 값 |
|---------|------|------|-----|
| `provider` | String | 필수 | `google`, `kakao`, `naver` (대소문자 무관) |

#### Response 200

```json
{
  "success": true,
  "message": "GOOGLE 계정 연결이 해제되었습니다.",
  "timestamp": "2026-02-14T10:30:00"
}
```

#### Error Cases

| 에러코드 | HTTP | 설명 |
|---------|------|------|
| `AC006` | 400 | 마지막 로그인 수단 해제 불가 (비밀번호 미설정 + SNS 1개만 남음) |
| `AC007` | 404 | 연결되지 않은 SNS 계정 |
| `OA001` | 400 | 지원하지 않는 SNS 제공자 |

#### 프론트엔드 주의사항

- 마지막 SNS 계정 해제 시도 시, 에러 메시지: `"마지막 SNS 연결입니다. 비밀번호를 먼저 설정해주세요."`
- 해제 전 확인 다이얼로그 표시 권장

---

## 7. 알림 시스템

### 7-1. 알림 목록 조회

#### `GET /api/notifications`

현재 사용자의 알림 목록을 조회합니다.

**인증**: Bearer Token 필요

#### Query Parameters

| 파라미터 | 타입 | 기본값 | 설명 |
|---------|------|-------|------|
| `page` | int | 1 | 페이지 번호 (1부터 시작) |
| `size` | int | 20 | 페이지 크기 |
| `unreadOnly` | boolean | false | `true`: 읽지 않은 알림만 |

#### Response 200

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": 1,
        "type": "RESERVATION_NEW",
        "title": "새 예약이 등록되었습니다",
        "message": "김미소 고객님이 2026-02-15 14:00 예약을 등록했습니다",
        "read": false,
        "link": "/shop-admin/reservations/list",
        "createdAt": "2026-02-14 10:30:00"
      },
      {
        "id": 2,
        "type": "RESERVATION_CANCELLED",
        "title": "예약이 취소되었습니다",
        "message": "박철수 고객님의 2026-02-15 16:00 예약이 취소되었습니다",
        "read": true,
        "link": "/shop-admin/reservations/list",
        "createdAt": "2026-02-14 09:00:00"
      }
    ],
    "totalCount": 15,
    "unreadCount": 3
  },
  "timestamp": "2026-02-14T10:30:00"
}
```

#### 알림 타입 (NotificationType)

| 타입 | 설명 | 자동 생성 시점 |
|------|------|--------------|
| `RESERVATION_NEW` | 새 예약 | 예약 생성 시 |
| `RESERVATION_CONFIRMED` | 예약 확정 | 예약 확정(confirm) 시 |
| `RESERVATION_CANCELLED` | 예약 취소 | 예약 취소(cancel) 시 |
| `RESERVATION_COMPLETED` | 예약 완료 | 예약 완료(complete) 시 |
| `RESERVATION_NO_SHOW` | 예약 노쇼 | 노쇼 처리 시 |
| `SYSTEM` | 시스템 알림 | 수동 생성 |

> 예약 관련 알림은 **매장 소유자(OWNER)** 에게 자동으로 생성됩니다.

#### 프론트엔드 연동 예시 (REST Polling)

```javascript
// NavBarNotifications.vue에서 주기적 조회
const fetchNotifications = async () => {
  const response = await axios.get('/api/notifications', {
    params: { page: 1, size: 10, unreadOnly: false }
  })
  notifications.value = response.data.data.items
  unreadCount.value = response.data.data.unreadCount
}

// 30초 간격 polling
setInterval(fetchNotifications, 30000)
onMounted(fetchNotifications)
```

### 7-2. 알림 읽음 처리

#### `PATCH /api/notifications/{id}/read`

특정 알림을 읽음으로 표시합니다.

**인증**: Bearer Token 필요

#### Path Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `id` | Long | 필수 | 알림 ID |

#### Response 200

```json
{
  "success": true,
  "message": "읽음 처리되었습니다.",
  "timestamp": "2026-02-14T10:30:00"
}
```

#### Error Cases

| 에러코드 | HTTP | 설명 |
|---------|------|------|
| `NT001` | 404 | 알림을 찾을 수 없음 |
| `NT002` | 403 | 해당 알림에 접근 권한 없음 (다른 사용자의 알림) |

### 7-3. 전체 알림 읽음 처리

#### `PATCH /api/notifications/read-all`

현재 사용자의 모든 알림을 읽음으로 표시합니다.

**인증**: Bearer Token 필요

#### Response 200

```json
{
  "success": true,
  "message": "모든 알림을 읽음 처리했습니다.",
  "timestamp": "2026-02-14T10:30:00"
}
```

---

## 8. 기존 API 확인 사항

### 8-1. OAuth/SNS 로그인 (기존 완료)

OAuth2 설정은 **이미 완료**되어 있습니다. `application.yml`에 Google, Kakao, Naver 설정이 포함되어 있습니다.

#### OAuth 플로우 (프론트엔드 기대 동작과 일치)

```
1. 프론트: window.location.href = '/api/oauth2/authorize/{provider}?redirect_uri={frontendUrl}/oauth2-redirect'
2. 백엔드: OAuth Provider로 리다이렉트
3. Provider: 인증 후 백엔드 callback URL로 리다이렉트
4. 백엔드: JWT 발급 후 프론트 redirect_uri로 리다이렉트
   → {frontendUrl}/oauth2-redirect?token={accessToken}&refreshToken={refreshToken}
5. 프론트: URL 파라미터에서 토큰 추출 → localStorage 저장 → 대시보드 이동
```

#### 동작 정책

| 항목 | 정책 |
|------|------|
| 기존 이메일과 SNS 연결 | 동일 이메일 기반 **자동 연결** |
| 첫 SNS 로그인 시 | **자동 회원가입** + 매장 자동 생성 |
| SNS 회원가입 시 비밀번호 | 랜덤 UUID 비밀번호 설정 (직접 로그인 불가) |

### 8-2. 비밀번호 찾기/재설정 (기존 완료)

SMTP 설정이 완료되어 있으며 아래와 같이 동작합니다.

#### `POST /api/auth/forgot-password`

```json
// Request
{ "email": "user@example.com" }

// Response 200
{
  "success": true,
  "message": "비밀번호 재설정 이메일을 발송했습니다. 이메일을 확인해주세요.",
  "timestamp": "2026-02-14T10:30:00"
}
```

#### `POST /api/auth/reset-password`

```json
// Request
{
  "token": "abc123-uuid-token",
  "newPassword": "newPw456"
}

// Response 200
{
  "success": true,
  "message": "비밀번호가 재설정되었습니다. 새 비밀번호로 로그인해주세요.",
  "timestamp": "2026-02-14T10:30:00"
}
```

#### 보안 정책

| 항목 | 설정값 |
|------|-------|
| 토큰 유효 시간 | 30분 |
| 토큰 재사용 | 1회 사용 후 즉시 무효화 |
| 이메일 형식 | `[MOER] 비밀번호 재설정 안내` |
| 재설정 링크 | `{frontendUrl}/reset-password?token={resetToken}` |

#### Error Cases

| 에러코드 | HTTP | 설명 |
|---------|------|------|
| `PR001` | 400 | 유효하지 않은 재설정 토큰 |
| `PR002` | 400 | 만료된 재설정 토큰 |
| `PR003` | 400 | 이미 사용된 재설정 토큰 |

---

## 9. 데이터 구조 참조

### 9-1. UserResponse (사용자 정보)

`GET /api/auth/me`, `PATCH /api/auth/profile` 등에서 반환되는 사용자 정보 구조입니다.

```typescript
interface UserResponse {
  id: number
  email: string
  name: string
  phone: string | null
  profileImageUrl: string | null    // Phase 1에서 추가됨
  role: 'OWNER' | 'STAFF' | 'ADMIN' | 'SUPER_ADMIN'
  roleDescription: string           // "매장 대표", "직원", etc.
  status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED' | 'DELETED'
  statusDescription: string         // "활성", "비활성", etc.
  staffId: number | null
  businessId: number | null
  emailVerified: boolean
  lastLoginAt: string | null        // "yyyy-MM-dd HH:mm:ss"
  createdAt: string                 // "yyyy-MM-dd HH:mm:ss"
  updatedAt: string                 // "yyyy-MM-dd HH:mm:ss"
}
```

### 9-2. NotificationResponse (알림)

```typescript
interface NotificationResponse {
  id: number
  type: 'RESERVATION_NEW' | 'RESERVATION_CONFIRMED' | 'RESERVATION_CANCELLED'
       | 'RESERVATION_COMPLETED' | 'RESERVATION_NO_SHOW' | 'SYSTEM'
  title: string
  message: string
  read: boolean
  link: string | null               // 클릭 시 이동 경로
  createdAt: string                 // "yyyy-MM-dd HH:mm:ss"
}

interface NotificationListResponse {
  items: NotificationResponse[]
  totalCount: number
  unreadCount: number
}
```

### 9-3. SnsAccountResponse (SNS 계정)

```typescript
interface SnsAccountResponse {
  provider: 'GOOGLE' | 'KAKAO' | 'NAVER'
  email: string | null
  name: string | null
  connectedAt: string              // "yyyy-MM-dd HH:mm:ss"
}
```

### 9-4. ProfileImageResponse (프로필 이미지)

```typescript
interface ProfileImageResponse {
  profileImageUrl: string           // "/uploads/profiles/uuid.jpg"
}
```

---

## 10. 에러코드 전체 목록 (Phase 1 신규)

### 프로필/계정 관련 (AC001 ~ AC008)

| 코드 | HTTP | 메시지 |
|------|------|--------|
| `AC001` | 400 | 현재 비밀번호가 일치하지 않습니다 |
| `AC002` | 400 | 새 비밀번호와 확인 비밀번호가 일치하지 않습니다 |
| `AC003` | 400 | 비밀번호는 8자 이상, 영문과 숫자를 포함해야 합니다 |
| `AC004` | 400 | 진행 중인 예약이 있어 탈퇴할 수 없습니다 |
| `AC005` | 400 | 이미 탈퇴된 계정입니다 |
| `AC006` | 400 | 마지막 로그인 수단은 해제할 수 없습니다 |
| `AC007` | 404 | 연결되지 않은 SNS 계정입니다 |
| `AC008` | 400 | 새 비밀번호는 현재 비밀번호와 달라야 합니다 |

### 파일 업로드 관련 (FI001 ~ FI005)

| 코드 | HTTP | 메시지 |
|------|------|--------|
| `FI001` | 500 | 파일 업로드에 실패했습니다 |
| `FI002` | 400 | 파일 크기가 제한을 초과했습니다 |
| `FI003` | 400 | 지원하지 않는 파일 형식입니다 |
| `FI004` | 404 | 파일을 찾을 수 없습니다 |
| `FI005` | 500 | 파일 삭제에 실패했습니다 |

### 알림 관련 (NT001 ~ NT002)

| 코드 | HTTP | 메시지 |
|------|------|--------|
| `NT001` | 404 | 알림을 찾을 수 없습니다 |
| `NT002` | 403 | 해당 알림에 접근 권한이 없습니다 |

---

## 11. 프론트엔드 API 연동 체크리스트

### auth.js 업데이트 필요 항목

```javascript
// 기존 API (변경 없음)
auth.login(email, password)           // POST /api/auth/login
auth.logout()                         // POST /api/auth/logout
auth.register(data)                   // POST /api/auth/register
auth.refreshToken(refreshToken)       // POST /api/auth/refresh
auth.getCurrentUser()                 // GET  /api/auth/me
auth.forgotPassword(email)            // POST /api/auth/forgot-password
auth.resetPassword(token, password)   // POST /api/auth/reset-password

// Phase 1 신규 API
auth.changePassword(data)             // POST   /api/auth/change-password
auth.updateProfile(data)              // PATCH  /api/auth/profile
auth.uploadProfileImage(file)         // POST   /api/auth/profile/image  (multipart)
auth.getSocialAccounts()              // GET    /api/auth/social-accounts
auth.disconnectSocialAccount(provider)// DELETE /api/auth/social-accounts/{provider}
auth.deleteAccount(data)              // DELETE /api/auth/account
```

### notifications.js 신규 생성 필요

```javascript
// Phase 1 신규 API
notifications.getNotifications(params) // GET   /api/notifications?page=&size=&unreadOnly=
notifications.markAsRead(id)           // PATCH /api/notifications/{id}/read
notifications.markAllAsRead()          // PATCH /api/notifications/read-all
```

---

## 12. DB 스키마 변경 사항 (참고)

### users 테이블

```sql
-- 추가된 컬럼
profile_image_url TEXT  -- 프로필 이미지 경로

-- 추가된 상태값
status: 'DELETED'  -- 기존 ACTIVE, INACTIVE, SUSPENDED에 추가
```

### notifications 테이블 (신규)

```sql
CREATE TABLE IF NOT EXISTS notifications (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id),
    business_id     BIGINT REFERENCES businesses(id),
    type            VARCHAR(30) NOT NULL,           -- NotificationType enum
    title           VARCHAR(200) NOT NULL,
    message         TEXT,
    link            VARCHAR(500),                   -- 클릭 시 이동 경로
    reference_type  VARCHAR(50),                    -- 참조 엔티티 타입
    reference_id    BIGINT,                         -- 참조 엔티티 ID
    is_read         CHAR(1) DEFAULT 'N',            -- Y/N
    read_at         TIMESTAMP,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

## 13. 참고 사항

### Swagger UI

모든 API는 Swagger UI에서 테스트 가능합니다.
- URL: `http://localhost:8080/swagger-ui.html`
- 인증이 필요한 API는 상단의 `Authorize` 버튼으로 Bearer Token을 설정하세요.

### 정적 리소스 (업로드된 파일)

- 업로드된 파일은 `/uploads/**` 경로로 접근 가능합니다.
- 인증 없이 접근 가능 (SecurityConfig에서 permitAll 설정됨)
- 개발 환경: 로컬 파일 시스템 (`./uploads/`)
- 운영 환경: S3 등 클라우드 스토리지로 전환 예정 (인터페이스 기반 설계)

### Phase 2 예고

Phase 2에서는 아래 기능이 추가될 예정입니다:
- 결제 시스템 통합 (PG사 연동)
- 쿠폰 관리
- 구독 플랜 변경
- 고객용 예약 UI (모바일)
