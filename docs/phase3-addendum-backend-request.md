# Phase 3 추가 요청서: 고객 인증 체계 및 포트폴리오 Public API

> 작성일: 2026-02-15
> 관련 문서: `phase3-backend-response.md`, `phase3-backend-request.md`
> 상태: **백엔드 개발 요청**

---

## 배경

Phase 3 초기 설계에서는 MVP 속도를 위해 **비회원 예약**(전화번호 + 예약번호 인증)으로 구현했으나,
운영 관점에서 아래 문제가 발생합니다:

| 문제 | 설명 |
|------|------|
| 본인 인증 미흡 | 전화번호만으로 예약/리뷰 가능 → 악의적 사용 가능 |
| 고객 이력 관리 불가 | 동일 고객이 매번 정보를 새로 입력 |
| 마케팅 불가 | 고객에게 푸시/알림 발송 수단 없음 |
| 리뷰 신뢰도 | 비로그인 리뷰는 신뢰도가 낮음 |

### 변경 방향

| 기능 | 현재 (Phase 3) | 변경 후 |
|------|----------------|---------|
| 매장 조회 | Public (인증 불필요) | **유지** |
| 포트폴리오 조회 | 미구현 | **Public (인증 불필요)** |
| 리뷰 목록 조회 | Public (인증 불필요) | **유지** |
| 예약 생성 | Public (비회원) | **고객 로그인 필요** |
| 예약 조회/취소 | Public (전화번호 인증) | **고객 로그인 필요** |
| 리뷰 작성 | Public (전화번호 인증) | **고객 로그인 필요** |

---

## 1. 고객 계정 체계 (CUSTOMER Role)

### 1-1. 설계 방향

기존 `users` 테이블에 `CUSTOMER` Role을 추가하는 방식을 권장합니다.

| 방안 | 장점 | 단점 |
|------|------|------|
| **A. 기존 users 테이블 + CUSTOMER Role** | 통합 인증 체계, OAuth 재사용 가능 | Role 구분 로직 필요 |
| B. 별도 customers 테이블 | 관리자/고객 완전 분리 | OAuth/JWT 이중 관리, 코드 중복 |

> **권장: 방안 A** — 기존 JWT 인프라와 OAuth 플로우를 그대로 활용 가능

### 1-2. DB 스키마 변경

```sql
-- Role enum에 CUSTOMER 추가
-- 기존: OWNER, STAFF, ADMIN, SUPER_ADMIN
-- 변경: OWNER, STAFF, ADMIN, SUPER_ADMIN, CUSTOMER

-- users 테이블에 고객 전용 필드 추가 (선택)
ALTER TABLE users ADD COLUMN phone VARCHAR(20);
ALTER TABLE users ADD COLUMN marketing_agree BOOLEAN DEFAULT FALSE;
ALTER TABLE users ADD COLUMN last_login_at TIMESTAMP;
```

### 1-3. JWT 토큰 구조 (고객용)

```json
{
  "sub": "customer@email.com",
  "id": 500,
  "name": "박서연",
  "role": "CUSTOMER",
  "phone": "010-9876-5432",
  "profileImageUrl": null,
  "iat": 1739600000,
  "exp": 1739686400
}
```

> **기존 Admin JWT와의 차이**: `role: "CUSTOMER"`, `phone` 필드 추가, `businessId`/`staffId` 없음

---

## 2. 고객 인증 API

### 2-1. 카카오 로그인 (Primary — 권장)

> 기존 OAuth2 인프라(`/oauth2/authorization/kakao`)를 고객용으로 확장

```
GET /oauth2/authorization/kakao?redirect_uri=/booking/{slug}&user_type=customer
```

**Flow:**
```
1. 고객이 /booking/{slug} 에서 "예약하기" 클릭
2. 로그인 안된 상태 → /booking/login?redirect=/booking/{slug}/reserve 로 이동
3. "카카오로 시작하기" 버튼 클릭
4. 카카오 OAuth 동의 → 콜백 → JWT 발급
5. redirect URL로 복귀 (로그인 완료 상태)
```

**카카오 OAuth 콜백 처리:**
```
GET /oauth2/callback/kakao?code={code}&state={state}

Response 200:
{
  "status": "success",
  "data": {
    "accessToken": "eyJ...",
    "refreshToken": "eyJ...",
    "isNewUser": true,
    "user": {
      "id": 500,
      "name": "박서연",
      "email": "soyeon@kakao.com",
      "phone": null,
      "profileImageUrl": "https://k.kakaocdn.net/...",
      "role": "CUSTOMER"
    }
  }
}
```

**신규 고객 처리 로직:**
- 카카오 계정으로 최초 로그인 시 `isNewUser: true`
- `users` 테이블에 `role=CUSTOMER`로 자동 등록
- 전화번호는 카카오에서 제공 시 자동 저장, 미제공 시 예약 진행 중 입력 요청

### 2-2. 전화번호 간편 로그인 (Secondary)

> 카카오 미사용 고객을 위한 대안

```
POST /api/public/auth/phone/send-code
Content-Type: application/json

Request Body:
{
  "phone": "010-9876-5432",
  "name": "박서연"
}

Response 200:
{
  "status": "success",
  "data": {
    "verificationId": "uuid-xxxx",
    "expiresIn": 180
  }
}
```

```
POST /api/public/auth/phone/verify
Content-Type: application/json

Request Body:
{
  "verificationId": "uuid-xxxx",
  "code": "123456"
}

Response 200:
{
  "status": "success",
  "data": {
    "accessToken": "eyJ...",
    "refreshToken": "eyJ...",
    "isNewUser": false,
    "user": {
      "id": 500,
      "name": "박서연",
      "phone": "010-9876-5432",
      "role": "CUSTOMER"
    }
  }
}
```

**SMS 인증 인프라:**
- 인증번호 6자리, 3분 유효
- 1일 최대 5회 발송 제한
- 인증 완료 시 users 테이블에 CUSTOMER로 자동 등록 (최초) 또는 기존 계정으로 로그인

> **Phase 3 단계에서는 카카오 로그인만 우선 구현하고, 전화번호 로그인은 Phase 4에서 추가해도 무방합니다.**

### 2-3. 고객 프로필 조회/수정

```
GET /api/customer/profile
Authorization: Bearer {customerToken}

Response 200:
{
  "status": "success",
  "data": {
    "id": 500,
    "name": "박서연",
    "email": "soyeon@kakao.com",
    "phone": "010-9876-5432",
    "profileImageUrl": "https://k.kakaocdn.net/...",
    "marketingAgree": false,
    "reservationCount": 5,
    "reviewCount": 2,
    "createdAt": "2026-02-15T10:00:00"
  }
}
```

```
PATCH /api/customer/profile
Authorization: Bearer {customerToken}
Content-Type: application/json

Request Body:
{
  "name": "박서연",
  "phone": "010-9876-5432",
  "marketingAgree": true
}

Response 200:
{
  "status": "success"
}
```

---

## 3. 예약 API 변경 (인증 필수)

### 3-1. 예약 생성 — 인증 방식 변경

```
POST /api/customer/businesses/{slug}/reservations
Authorization: Bearer {customerToken}
Content-Type: application/json

Request Body:
{
  "serviceIds": [1],
  "staffId": 1,
  "reservationDate": "2026-02-20",
  "startTime": "14:00",
  "customerRequest": "조용한 자리 부탁드려요"
}
```

**변경 사항:**
- URL: `/api/public/businesses/{slug}/reservations` → `/api/customer/businesses/{slug}/reservations`
- 인증: 불필요 → `Bearer {customerToken}` 필수
- Body에서 `customerName`, `customerPhone`, `customerEmail` 제거 (JWT에서 추출)
- 고객 전화번호가 없는 경우 → 에러 반환 (프로필에서 전화번호 먼저 등록 요청)

**Response 201:** (기존과 동일)
```json
{
  "status": "success",
  "data": {
    "reservationNumber": "260220-A3B9",
    "status": "CONFIRMED",
    "reservationDate": "2026-02-20",
    "startTime": "14:00",
    "endTime": "15:00",
    "message": "예약이 완료되었습니다."
  }
}
```

**추가 에러 코드:**
| 코드 | HTTP | 설명 |
|------|------|------|
| CU001 | 400 | 고객 전화번호 미등록 (프로필에서 먼저 등록 필요) |

### 3-2. 내 예약 목록 조회 (신규)

```
GET /api/customer/reservations
Authorization: Bearer {customerToken}

Query Parameters:
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| status | String | X | - | PENDING, CONFIRMED, COMPLETED, CANCELLED |
| page | Integer | X | 1 | 페이지 번호 |
| size | Integer | X | 10 | 페이지 크기 |

Response 200:
{
  "status": "success",
  "data": {
    "items": [
      {
        "reservationNumber": "260220-A3B9",
        "status": "CONFIRMED",
        "businessName": "미소 헤어",
        "businessSlug": "miso-hair",
        "businessProfileImageUrl": "/uploads/businesses/1_profile.jpg",
        "reservationDate": "2026-02-20",
        "startTime": "14:00",
        "endTime": "15:00",
        "staffName": "김디자이너",
        "services": ["여성컷"],
        "totalPrice": 30000,
        "canCancel": true,
        "hasReview": false,
        "createdAt": "2026-02-14T10:30:00"
      }
    ],
    "totalCount": 5
  }
}
```

### 3-3. 예약 상세 조회 — 인증 방식 변경

```
GET /api/customer/reservations/{reservationNumber}
Authorization: Bearer {customerToken}

Response 200:
(기존 /api/public/reservations/{reservationNumber} 응답과 동일,
 phone 파라미터 불필요 — JWT로 본인 확인)
```

### 3-4. 예약 취소 — 인증 방식 변경

```
POST /api/customer/reservations/{reservationNumber}/cancel
Authorization: Bearer {customerToken}
Content-Type: application/json

Request Body:
{
  "reason": "일정 변경"
}
```

**변경 사항:**
- URL: `/api/public/reservations/{reservationNumber}/cancel` → `/api/customer/reservations/{reservationNumber}/cancel`
- Body에서 `phone` 제거 (JWT로 본인 확인)

---

## 4. 리뷰 API 변경 (인증 필수)

### 4-1. 리뷰 작성 — 인증 방식 변경

```
POST /api/customer/businesses/{slug}/reviews
Authorization: Bearer {customerToken}
Content-Type: application/json

Request Body:
{
  "reservationNumber": "260220-A3B9",
  "rating": 5,
  "content": "정말 만족스러웠어요!",
  "staffId": 1
}
```

**변경 사항:**
- URL: `/api/public/businesses/{slug}/reviews` → `/api/customer/businesses/{slug}/reviews`
- Body에서 `phone` 제거 (JWT로 본인 확인)
- `reservationNumber`만으로 예약 → 고객 매칭 확인

**기존 에러 코드 유지:**
| 코드 | HTTP | 설명 |
|------|------|------|
| RV001 | 400 | 예약이 해당 고객의 것이 아님 |
| RV002 | 400 | 완료되지 않은 예약 |
| RV003 | 409 | 이미 리뷰 작성됨 |

### 4-2. 리뷰 목록 조회 (Public — 변경 없음)

```
GET /api/public/businesses/{slug}/reviews
(인증 불필요 — 기존과 동일)
```

---

## 5. 포트폴리오 Public API (신규)

> 현재 포트폴리오 API는 Admin 인증 전용(`/api/businesses/{businessId}/staffs/{staffId}/portfolios`)만 존재합니다.
> 고객이 매장 페이지에서 스태프 포트폴리오를 조회할 수 있도록 Public API가 필요합니다.

### 5-1. 스태프 포트폴리오 목록 조회 (Public)

```
GET /api/public/businesses/{slug}/staffs/{staffId}/portfolios
인증: 불필요
```

**Query Parameters:**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| page | Integer | X | 1 | 페이지 번호 |
| size | Integer | X | 20 | 페이지 크기 |

**Response 200:**
```json
{
  "status": "success",
  "data": {
    "staffName": "김디자이너",
    "staffPosition": "원장",
    "items": [
      {
        "id": 1,
        "imageUrl": "/uploads/portfolios/1_img01.jpg",
        "thumbnailUrl": "/uploads/portfolios/1_img01_thumb.jpg",
        "title": "봄 시즌 펌 스타일",
        "description": "자연스러운 웨이브 펌",
        "serviceName": "디지털펌",
        "createdAt": "2026-02-01T10:00:00"
      },
      {
        "id": 2,
        "imageUrl": "/uploads/portfolios/1_img02.jpg",
        "thumbnailUrl": "/uploads/portfolios/1_img02_thumb.jpg",
        "title": "레이어드 커트",
        "description": null,
        "serviceName": "여성컷",
        "createdAt": "2026-01-28T14:00:00"
      }
    ],
    "totalCount": 23
  }
}
```

**참고:**
- Admin 포트폴리오 API의 응답 필드를 그대로 활용하되, `staffName`/`staffPosition` 추가
- `thumbnailUrl`이 없으면 `imageUrl`을 직접 사용 (프론트에서 처리)
- ACTIVE 상태의 포트폴리오만 노출

---

## 6. Security 설정 변경

### 6-1. URL 패턴별 인증 정책

```java
// SecurityConfig.java

// Public (인증 불필요) — 기존 유지 + 포트폴리오 추가
.requestMatchers("/api/public/**").permitAll()

// Customer (고객 JWT 필요) — 신규
.requestMatchers("/api/customer/**").hasRole("CUSTOMER")

// Admin (관리자 JWT 필요) — 기존 유지
.requestMatchers("/api/businesses/**").hasAnyRole("OWNER", "STAFF", "ADMIN", "SUPER_ADMIN")
```

### 6-2. 기존 Public 예약/리뷰 API 처리

| 기존 API | 처리 방안 |
|----------|----------|
| `POST /api/public/.../reservations` | **Deprecated → 제거** 또는 비활성화 |
| `GET /api/public/reservations/{no}?phone=` | **Deprecated → 제거** 또는 비활성화 |
| `POST /api/public/reservations/{no}/cancel` | **Deprecated → 제거** 또는 비활성화 |
| `POST /api/public/.../reviews` | **Deprecated → 제거** 또는 비활성화 |

> 기존 비회원 API를 즉시 제거하지 않고, 과도기에는 양쪽 모두 운영할 수도 있습니다.
> 다만 최종적으로는 `/api/customer/` 기반 인증 API만 사용하는 것을 권장합니다.

---

## 7. 카카오 로그인 구현 상세

### 7-1. 현재 상태

| 항목 | 상태 | 비고 |
|------|------|------|
| 카카오 앱 등록 | 확인 필요 | Kakao Developers 콘솔 |
| Spring Security OAuth2 Client | 설정 존재 | `/oauth2/authorization/kakao` 엔드포인트 확인 |
| 프론트엔드 버튼 UI | 구현 완료 | 로그인 페이지에 소셜 버튼 존재 |
| 콜백 처리 페이지 | 구현 완료 | `src/pages/oauth2-redirect.vue` |
| 고객/관리자 분기 로직 | **미구현** | 신규 필요 |

### 7-2. 카카오 로그인 분기 처리

같은 카카오 계정이라도 **관리자 로그인**과 **고객 로그인**은 분기해야 합니다.

```
# 관리자용 (기존)
GET /oauth2/authorization/kakao?user_type=admin

# 고객용 (신규)
GET /oauth2/authorization/kakao?user_type=customer
```

**OAuth2 콜백 처리 로직:**
```
1. 카카오에서 profile 수신 (email, name, phone, profileImage)
2. user_type 파라미터 확인
   - "admin" → 기존 로직 (OWNER/ADMIN 매칭)
   - "customer" → 고객 로직 (아래)
3. 고객 로직:
   a. email로 기존 CUSTOMER 계정 조회
   b. 없으면 → users 테이블에 role=CUSTOMER로 자동 등록
   c. 있으면 → 기존 계정으로 로그인
4. JWT 발급 (role=CUSTOMER)
5. redirect_uri로 리다이렉트 (accessToken, refreshToken을 쿼리 파라미터 또는 fragment로 전달)
```

### 7-3. Kakao에서 제공하는 사용자 정보

| 필드 | 동의 항목 | 필수 여부 |
|------|----------|----------|
| 닉네임 | 필수 동의 | **필수** |
| 프로필 이미지 | 필수 동의 | 선택 |
| 이메일 | 선택 동의 | 권장 |
| 전화번호 | 선택 동의 (비즈앱) | **권장** (예약에 필요) |

> **비즈앱 전환** 시 전화번호 수집 가능. 비즈앱이 아닌 경우 전화번호는 프로필에서 수동 입력 필요.

---

## 8. 프론트엔드 변경 사항 (참고)

> 백엔드 API 구현 완료 후 프론트엔드에서 처리할 사항입니다.

### 8-1. 고객 로그인 페이지 신설

- **경로**: `/booking/login`
- **기능**: 카카오 로그인 버튼 + (향후) 전화번호 로그인
- **redirect 처리**: 로그인 후 원래 페이지로 복귀

### 8-2. 매장 페이지 UI 변경

```
/booking/{slug} 페이지:

[비로그인 상태]
- 매장 정보 조회 ✓ (Public)
- 서비스 목록 조회 ✓ (Public)
- 스태프 목록 조회 ✓ (Public)
- 포트폴리오 조회 ✓ (Public)
- 리뷰 목록 조회 ✓ (Public)
- "예약하기" 클릭 → /booking/login?redirect=... 으로 이동
- "리뷰 작성" 클릭 → /booking/login?redirect=... 으로 이동

[로그인 상태]
- 위 모든 기능 + 예약 생성 + 리뷰 작성 가능
```

### 8-3. 고객 토큰 관리

- 관리자 토큰(`accessToken`)과 고객 토큰(`customerAccessToken`)은 별도 키로 localStorage에 저장
- axios interceptor에서 `/api/customer/` 요청 시 `customerAccessToken` 사용
- 고객과 관리자가 동시에 로그인 가능 (서로 다른 토큰)

---

## 9. API 엔드포인트 요약

### 신규 Public API

| Method | URL | 설명 |
|--------|-----|------|
| `GET` | `/api/public/businesses/{slug}/staffs/{staffId}/portfolios` | 스태프 포트폴리오 조회 |

### 신규 Customer Auth API

| Method | URL | 설명 |
|--------|-----|------|
| `GET` | `/oauth2/authorization/kakao?user_type=customer` | 카카오 고객 로그인 |
| `POST` | `/api/public/auth/phone/send-code` | 전화번호 인증코드 발송 (Phase 4) |
| `POST` | `/api/public/auth/phone/verify` | 전화번호 인증 확인 (Phase 4) |

### 신규 Customer API (인증 필요)

| Method | URL | 설명 |
|--------|-----|------|
| `GET` | `/api/customer/profile` | 고객 프로필 조회 |
| `PATCH` | `/api/customer/profile` | 고객 프로필 수정 |
| `POST` | `/api/customer/businesses/{slug}/reservations` | 예약 생성 |
| `GET` | `/api/customer/reservations` | 내 예약 목록 |
| `GET` | `/api/customer/reservations/{reservationNumber}` | 예약 상세 조회 |
| `POST` | `/api/customer/reservations/{reservationNumber}/cancel` | 예약 취소 |
| `POST` | `/api/customer/businesses/{slug}/reviews` | 리뷰 작성 |

### Deprecated (제거 예정)

| Method | URL | 대체 API |
|--------|-----|----------|
| `POST` | `/api/public/businesses/{slug}/reservations` | `/api/customer/businesses/{slug}/reservations` |
| `GET` | `/api/public/reservations/{no}?phone=` | `/api/customer/reservations/{no}` |
| `POST` | `/api/public/reservations/{no}/cancel` | `/api/customer/reservations/{no}/cancel` |
| `POST` | `/api/public/businesses/{slug}/reviews` | `/api/customer/businesses/{slug}/reviews` |

---

## 10. 에러 코드 추가

### 고객 인증 (CU001 ~ CU006)

| 코드 | HTTP | 메시지 |
|------|------|--------|
| CU001 | 400 | 고객 전화번호가 등록되지 않았습니다 |
| CU002 | 400 | 인증번호가 일치하지 않습니다 |
| CU003 | 400 | 인증번호가 만료되었습니다 |
| CU004 | 429 | 인증번호 발송 횟수를 초과했습니다 (1일 5회) |
| CU005 | 401 | 고객 인증이 필요합니다 |
| CU006 | 403 | 고객 권한으로만 접근 가능합니다 |

---

## 11. 구현 우선순위

| 순번 | 작업 | 규모 | 의존성 | 비고 |
|------|------|------|--------|------|
| 1 | CUSTOMER Role 추가 + DB 마이그레이션 | 소 | 없음 | users 테이블 수정 |
| 2 | 카카오 OAuth 고객/관리자 분기 처리 | 중 | #1 | 기존 OAuth 확장 |
| 3 | 포트폴리오 Public API | 소 | 없음 | 기존 Admin API 참조 |
| 4 | 고객 프로필 API | 소 | #1 | CRUD |
| 5 | 예약 Customer API (`/api/customer/...`) | 중 | #1, #2 | 기존 로직 재사용 |
| 6 | 리뷰 Customer API (`/api/customer/...`) | 소 | #1, #2 | 기존 로직 재사용 |
| 7 | 내 예약 목록 API | 소 | #5 | 신규 |
| 8 | SecurityConfig 업데이트 | 소 | #1~#7 | URL 패턴 권한 설정 |
| 9 | 전화번호 SMS 인증 (Phase 4) | 중 | SMS 인프라 | 후순위 |

---

## 12. 아키텍처 결정 필요 사항

- [ ] **고객 계정 방식**: 기존 users 테이블 + CUSTOMER Role (권장) vs 별도 테이블
- [ ] **카카오 비즈앱 전환**: 전화번호 수집이 필요하면 비즈앱 필수
- [ ] **기존 비회원 API 유지 여부**: 즉시 제거 vs 과도기 양쪽 운영
- [ ] **전화번호 로그인 시점**: Phase 3에 포함 vs Phase 4로 이관
- [ ] **고객 토큰 분리**: 관리자/고객 토큰을 같은 키로 관리 vs 별도 키 사용
