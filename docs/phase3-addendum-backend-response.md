# Phase 3 Addendum: 백엔드 API 응답서 -- 고객 인증 체계 및 포트폴리오 Public API

> 작성일: 2026-02-16
> 요청 문서: `docs/phase3-addendum-backend-request.md`
> 기반 문서: `docs/phase3-backend-response.md`
> 상태: **구현 완료**

---

## 아키텍처 결정 사항

| 항목 | 결정 | 근거 |
|------|------|------|
| 고객 계정 체계 | **기존 users 테이블 + CUSTOMER Role** | OAuth2 인프라 재사용, 통합 JWT 인증 |
| 카카오 로그인 분기 | **loginType 쿠키 기반** | 쿼리 파라미터 `loginType`을 쿠키로 전달, OAuth 콜백에서 분기 |
| marketing_agree 타입 | **CHAR(1) ('Y'/'N')** | 기존 DB 컨벤션 유지 |
| 비회원 예약 API | **유지 (Deprecated 아님)** | 과도기 양쪽 운영, 기존 프론트엔드 호환성 보장 |
| 리뷰 작성 | **고객 로그인 필수** | Public POST 제거, userId 기반 본인 확인으로 신뢰도 향상 |
| 전화번호 로그인 | **Phase 4로 이관** | SMS 인프라 미구축, 카카오 로그인으로 우선 커버 |

---

## 변경 요약

### Phase 3 대비 변경점

| 영역 | Phase 3 | Phase 3 Addendum |
|------|---------|------------------|
| 고객 인증 | 비회원 (전화번호+예약번호) | **CUSTOMER 로그인 (JWT)** |
| 예약 생성 (고객) | `POST /api/public/.../reservations` | **`POST /api/customer/.../reservations`** |
| 예약 조회 (고객) | `GET /api/public/reservations/{no}?phone=` | **`GET /api/customer/reservations/{no}`** |
| 예약 취소 (고객) | `POST /api/public/reservations/{no}/cancel` | **`POST /api/customer/reservations/{no}/cancel`** |
| 리뷰 작성 | `POST /api/public/.../reviews` | **`POST /api/customer/.../reviews`** |
| 포트폴리오 조회 | Admin 전용 | **Public API 추가** |
| 고객 프로필 | 없음 | **신규 (GET/PATCH)** |
| OAuth 분기 | 관리자 전용 | **관리자/고객 분기 처리** |

---

## 1. CUSTOMER Role + DB 스키마 변경

### 1-1. UserRole 변경

```java
// UserRole.java
public enum UserRole {
    SUPER_ADMIN,
    ADMIN,
    OWNER,
    STAFF,
    CUSTOMER  // 신규 추가
}
```

### 1-2. User Entity 변경

```java
// User.java에 추가된 필드/메서드

private String marketingAgree;  // 'Y' 또는 'N'

public boolean isCustomer() {
    return this.role == UserRole.CUSTOMER;
}
```

### 1-3. DB 스키마 변경

```sql
-- users 테이블
ALTER TABLE users ADD COLUMN marketing_agree CHAR(1) DEFAULT 'N';

-- reservations 테이블 (로그인 고객 추적)
ALTER TABLE reservations ADD COLUMN user_id BIGINT;
CREATE INDEX idx_reservations_user_id ON reservations(user_id);

-- customers 테이블 (사용자 계정 연동)
ALTER TABLE customers ADD COLUMN user_id BIGINT;
```

### 1-4. MyBatis 매핑 변경

**UserMapper.xml 추가 쿼리:**

| 쿼리 ID | 설명 |
|----------|------|
| `updateCustomerProfile` | 고객 프로필 수정 (name, phone, marketingAgree) |
| `countReservationsByUserId` | 고객의 총 예약 수 |
| `countReviewsByUserId` | 고객의 총 리뷰 수 |

**ReservationMapper.xml 추가 쿼리:**

| 쿼리 ID | 설명 |
|----------|------|
| `findByUserId` | userId로 예약 목록 조회 |
| `countByUserId` | userId로 예약 수 카운트 |
| `findByUserIdAndReservationNumber` | userId + 예약번호로 예약 조회 |
| `updateUserId` | 예약에 userId 연동 |

**CustomerMapper.xml 추가 쿼리:**

| 쿼리 ID | 설명 |
|----------|------|
| `findByUserIdAndBusinessId` | userId + businessId로 고객 조회 |
| `updateUserId` | 고객에 userId 연동 |

---

## 2. OAuth2 고객/관리자 분기 처리

### 2-1. 로그인 URL

```
# 관리자 로그인
GET /oauth2/authorize/kakao?loginType=admin

# 고객 로그인
GET /oauth2/authorize/kakao?loginType=customer
```

### 2-2. 구현 컴포넌트

| 파일 | 상태 | 설명 |
|------|------|------|
| `CustomAuthorizationRequestResolver.java` | **신규** | `loginType` 쿼리 파라미터를 `moer_login_type` 쿠키로 저장 |
| `CustomOAuth2UserService.java` | **수정** | 쿠키에서 loginType 읽어 CUSTOMER/OWNER 분기 |
| `OAuth2AuthenticationSuccessHandler.java` | **수정** | loginType에 따라 고객/관리자 리다이렉트 URI 분기 |
| `SecurityConfig.java` | **수정** | `CustomAuthorizationRequestResolver` 등록 |
| `application.yml` | **수정** | `app.oauth2.customer-redirect-uri` 추가 |

### 2-3. OAuth 플로우

```
[관리자 로그인]
1. 프론트: /oauth2/authorize/kakao?loginType=admin 호출
2. CustomAuthorizationRequestResolver: moer_login_type=admin 쿠키 설정
3. 카카오 인증 완료
4. CustomOAuth2UserService: loginType=admin -> OWNER Role, Business/Trial 자동 생성
5. OAuth2AuthenticationSuccessHandler: 관리자 리다이렉트 URI로 이동
6. 리다이렉트: {admin-redirect-uri}?accessToken=...&refreshToken=...&isNewUser=...

[고객 로그인]
1. 프론트: /oauth2/authorize/kakao?loginType=customer 호출
2. CustomAuthorizationRequestResolver: moer_login_type=customer 쿠키 설정
3. 카카오 인증 완료
4. CustomOAuth2UserService: loginType=customer -> CUSTOMER Role, Business 생성 없음
5. OAuth2AuthenticationSuccessHandler: 고객 리다이렉트 URI로 이동
6. 리다이렉트: {customer-redirect-uri}?accessToken=...&refreshToken=...&isNewUser=...
```

### 2-4. isNewUser 플래그

- OAuth 콜백 리다이렉트 시 `isNewUser=true/false` 쿼리 파라미터 포함
- 프론트엔드에서 신규 사용자 안내 페이지 표시에 활용

---

## 3. 포트폴리오 Public API

### 3-1. 스태프 포트폴리오 목록 조회

```
GET /api/public/businesses/{slug}/staffs/{staffId}/portfolios
인증: 불필요
```

**Query Parameters:**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| `page` | Integer | X | 1 | 페이지 번호 |
| `size` | Integer | X | 20 | 페이지 크기 |

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
      }
    ],
    "totalCount": 23
  }
}
```

**구현 파일:**

| 파일 | 상태 | 설명 |
|------|------|------|
| `PublicPortfolioController.java` | **신규** | GET 엔드포인트 |
| `PublicPortfolioListResponse.java` | **신규** | staffName, staffPosition, items, totalCount |

---

## 4. 고객 프로필 API

### 4-1. 프로필 조회

```
GET /api/customer/profile
Authorization: Bearer {customerToken}
```

**Response 200:**
```json
{
  "status": "success",
  "data": {
    "id": 500,
    "name": "박서연",
    "email": "soyeon@kakao.com",
    "phone": "010-9876-5432",
    "profileImageUrl": "https://k.kakaocdn.net/...",
    "marketingAgree": "N",
    "reservationCount": 5,
    "reviewCount": 2,
    "createdAt": "2026-02-15T10:00:00"
  }
}
```

### 4-2. 프로필 수정

```
PATCH /api/customer/profile
Authorization: Bearer {customerToken}
Content-Type: application/json
```

**Request Body:**
```json
{
  "name": "박서연",
  "phone": "010-9876-5432",
  "marketingAgree": "Y"
}
```

**Response 200:**
```json
{
  "status": "success"
}
```

**구현 파일:**

| 파일 | 상태 | 설명 |
|------|------|------|
| `CustomerProfileController.java` | **신규** | GET/PATCH 엔드포인트 |
| `CustomerProfileService.java` | **신규** | getProfile, updateProfile |
| `CustomerProfileResponse.java` | **신규** | 프로필 응답 DTO |
| `CustomerProfileUpdateRequest.java` | **신규** | 프로필 수정 요청 DTO |

---

## 5. 고객 예약 API

### 5-1. 예약 생성

```
POST /api/customer/businesses/{slug}/reservations
Authorization: Bearer {customerToken}
Content-Type: application/json
```

**Request Body:**
```json
{
  "serviceIds": [1],
  "staffId": 1,
  "reservationDate": "2026-02-20",
  "startTime": "14:00",
  "customerRequest": "조용한 자리 부탁드려요"
}
```

**Phase 3 대비 변경:**
- `customerName`, `customerPhone`, `customerEmail` 제거 (JWT에서 추출)
- 고객 전화번호 미등록 시 에러 반환 (CP001)

**Response 201:**
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

### 5-2. 내 예약 목록

```
GET /api/customer/reservations
Authorization: Bearer {customerToken}
```

**Query Parameters:**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| `status` | String | X | - | PENDING, CONFIRMED, COMPLETED, CANCELLED |
| `page` | Integer | X | 1 | 페이지 번호 |
| `size` | Integer | X | 10 | 페이지 크기 |

**Response 200:**
```json
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

### 5-3. 예약 상세 조회

```
GET /api/customer/reservations/{reservationNumber}
Authorization: Bearer {customerToken}
```

**Phase 3 대비 변경:**
- `phone` 쿼리 파라미터 불필요 (JWT로 본인 확인)

**Response 200:** (Phase 3의 예약 조회 응답과 동일한 구조)

### 5-4. 예약 취소

```
POST /api/customer/reservations/{reservationNumber}/cancel
Authorization: Bearer {customerToken}
Content-Type: application/json
```

**Request Body:**
```json
{
  "reason": "일정 변경"
}
```

**Phase 3 대비 변경:**
- Body에서 `phone` 제거 (JWT로 본인 확인)

**Response 200:**
```json
{
  "status": "success"
}
```

**구현 파일:**

| 파일 | 상태 | 설명 |
|------|------|------|
| `CustomerBookingController.java` | **신규** | 4개 엔드포인트 |
| `CustomerBookingService.java` | **신규** | createReservation, getMyReservations, getReservation, cancelReservation |
| `CustomerReservationCreateRequest.java` | **신규** | serviceIds, staffId, reservationDate, startTime, customerRequest |
| `CustomerReservationListResponse.java` | **신규** | 예약 목록 응답 DTO (매장/스태프 정보 포함) |
| `CustomerReservationCancelRequest.java` | **신규** | reason |

---

## 6. 고객 리뷰 API

### 6-1. 리뷰 작성

```
POST /api/customer/businesses/{slug}/reviews
Authorization: Bearer {customerToken}
Content-Type: application/json
```

**Request Body:**
```json
{
  "reservationNumber": "260220-A3B9",
  "rating": 5,
  "content": "정말 만족스러웠어요!",
  "staffId": 1
}
```

**Phase 3 대비 변경:**
- `phone` 제거 (JWT의 userId로 본인 확인)
- userId 기반으로 해당 예약이 본인의 것인지 검증
- `POST /api/public/businesses/{slug}/reviews` 엔드포인트 **제거** (GET만 유지)

**Response 201:**
```json
{
  "status": "success",
  "data": {
    "id": 1,
    "customerName": "박*연",
    "rating": 5,
    "content": "정말 만족스러웠어요!",
    "serviceName": "여성컷",
    "staffName": "김디자이너",
    "images": [],
    "createdAt": "2026-02-16T14:00:00",
    "reply": null
  }
}
```

**Error Codes:**
| 코드 | HTTP | 설명 |
|------|------|------|
| RV001 | 400 | 예약 정보가 일치하지 않습니다 |
| RV002 | 400 | 완료된 예약만 리뷰를 작성할 수 있습니다 |
| RV003 | 409 | 이미 리뷰가 작성된 예약입니다 |
| CP004 | 403 | 본인의 예약에 대해서만 리뷰를 작성할 수 있습니다 |

**구현 파일:**

| 파일 | 상태 | 설명 |
|------|------|------|
| `CustomerReviewController.java` | **신규** | POST 엔드포인트 |
| `CustomerReviewCreateRequest.java` | **신규** | phone 없는 리뷰 요청 DTO |
| `ReviewService.java` | **수정** | `createReviewByCustomer()` 메서드 추가 |
| `PublicReviewController.java` | **수정** | POST 엔드포인트 제거, GET만 유지 |

---

## 7. Security 설정

### 7-1. URL 패턴별 인증 정책

```java
// SecurityConfig.java

// Public (인증 불필요)
.requestMatchers("/api/public/**").permitAll()

// Customer (고객 JWT 필요)
.requestMatchers("/api/customer/**").hasRole("CUSTOMER")

// Admin (관리자 JWT 필요)
.requestMatchers("/api/businesses/**").hasAnyRole("OWNER", "STAFF", "ADMIN", "SUPER_ADMIN")

// SuperAdmin
.requestMatchers("/api/super-admin/**").hasRole("SUPER_ADMIN")
```

### 7-2. 빌드 검증

- `./gradlew build` 성공 확인 완료

---

## 8. Error Code 전체 목록 (Phase 3 Addendum)

### 고객 프로필/예약/리뷰 (CP001 ~ CP004)

| 코드 | HTTP | 메시지 |
|------|------|--------|
| CP001 | 400 | 예약을 위해 전화번호 등록이 필요합니다 |
| CP002 | 403 | 고객 권한이 필요합니다 |
| CP003 | 404 | 고객의 예약을 찾을 수 없습니다 |
| CP004 | 403 | 본인의 예약에 대해서만 리뷰를 작성할 수 있습니다 |

---

## 9. API 엔드포인트 전체 요약

### Public API (인증 불필요)

| Method | URL | 설명 | 비고 |
|--------|-----|------|------|
| `GET` | `/api/public/businesses` | 매장 검색/목록 | Phase 3 |
| `GET` | `/api/public/businesses/check-slug` | 슬러그 확인 | Phase 3 |
| `GET` | `/api/public/businesses/{slug}` | 매장 상세 조회 | Phase 3 |
| `GET` | `/api/public/businesses/{slug}/available-dates` | 예약 가능 날짜 | Phase 3 |
| `GET` | `/api/public/businesses/{slug}/available-times` | 예약 가능 시간 | Phase 3 |
| `POST` | `/api/public/businesses/{slug}/reservations` | 비회원 예약 생성 | Phase 3 (유지) |
| `GET` | `/api/public/reservations/{reservationNumber}` | 비회원 예약 조회 | Phase 3 (유지) |
| `POST` | `/api/public/reservations/{reservationNumber}/cancel` | 비회원 예약 취소 | Phase 3 (유지) |
| `GET` | `/api/public/businesses/{slug}/reviews` | 리뷰 목록 조회 | Phase 3 |
| `GET` | `/api/public/businesses/{slug}/staffs/{staffId}/portfolios` | 포트폴리오 조회 | **Addendum 신규** |

### Customer API (JWT + CUSTOMER Role)

| Method | URL | 설명 | 비고 |
|--------|-----|------|------|
| `GET` | `/api/customer/profile` | 내 프로필 조회 | **Addendum 신규** |
| `PATCH` | `/api/customer/profile` | 내 프로필 수정 | **Addendum 신규** |
| `POST` | `/api/customer/businesses/{slug}/reservations` | 예약 생성 | **Addendum 신규** |
| `GET` | `/api/customer/reservations` | 내 예약 목록 | **Addendum 신규** |
| `GET` | `/api/customer/reservations/{reservationNumber}` | 예약 상세 | **Addendum 신규** |
| `POST` | `/api/customer/reservations/{reservationNumber}/cancel` | 예약 취소 | **Addendum 신규** |
| `POST` | `/api/customer/businesses/{slug}/reviews` | 리뷰 작성 | **Addendum 신규** |

### Admin API (JWT + OWNER/ADMIN/STAFF/SUPER_ADMIN)

변경 없음. Phase 3의 Admin API 그대로 유지.

### OAuth 엔드포인트

| Method | URL | 설명 | 비고 |
|--------|-----|------|------|
| `GET` | `/oauth2/authorize/kakao?loginType=admin` | 관리자 카카오 로그인 | 기존 확장 |
| `GET` | `/oauth2/authorize/kakao?loginType=customer` | 고객 카카오 로그인 | **Addendum 신규** |

---

## 10. 신규/변경 파일 목록

### 신규 파일

| 패키지 경로 | 파일명 | 설명 |
|-------------|--------|------|
| `domain.booking.controller` | `CustomerBookingController.java` | 고객 예약 API |
| `domain.booking.service` | `CustomerBookingService.java` | 고객 예약 비즈니스 로직 |
| `domain.booking.dto` | `CustomerReservationCreateRequest.java` | 예약 생성 요청 DTO |
| `domain.booking.dto` | `CustomerReservationListResponse.java` | 예약 목록 응답 DTO |
| `domain.booking.dto` | `CustomerReservationCancelRequest.java` | 예약 취소 요청 DTO |
| `domain.customer.controller` | `CustomerProfileController.java` | 고객 프로필 API |
| `domain.customer.service` | `CustomerProfileService.java` | 고객 프로필 비즈니스 로직 |
| `domain.customer.dto` | `CustomerProfileResponse.java` | 프로필 응답 DTO |
| `domain.customer.dto` | `CustomerProfileUpdateRequest.java` | 프로필 수정 요청 DTO |
| `domain.review.controller` | `CustomerReviewController.java` | 고객 리뷰 API |
| `domain.review.dto` | `CustomerReviewCreateRequest.java` | 리뷰 생성 요청 DTO |
| `domain.portfolio.controller` | `PublicPortfolioController.java` | 포트폴리오 Public API |
| `domain.portfolio.dto` | `PublicPortfolioListResponse.java` | 포트폴리오 목록 응답 DTO |
| `common.config` | `CustomAuthorizationRequestResolver.java` | OAuth loginType 쿠키 처리 |

### 변경 파일

| 파일 | 변경 내용 |
|------|----------|
| `domain/user/UserRole.java` | `CUSTOMER` 추가 |
| `domain/user/User.java` | `marketingAgree` 필드, `isCustomer()` 메서드 추가 |
| `common/exception/ErrorCode.java` | CP001~CP004 에러 코드 추가 |
| `common/config/SecurityConfig.java` | `/api/customer/**` 권한 설정, CustomAuthorizationRequestResolver 등록 |
| `common/config/CustomOAuth2UserService.java` | loginType 쿠키 읽기, CUSTOMER Role 분기 |
| `common/config/OAuth2AuthenticationSuccessHandler.java` | 고객/관리자 리다이렉트 URI 분기, isNewUser 플래그 |
| `application.yml` | `app.oauth2.customer-redirect-uri` 추가 |
| `resources/db/schema.sql` | users.marketing_agree, reservations.user_id, customers.user_id 컬럼 추가 |
| `resources/mapper/user/UserMapper.xml` | marketingAgree 매핑, updateCustomerProfile, count 쿼리 추가 |
| `resources/mapper/reservation/ReservationMapper.xml` | userId 매핑, findByUserId, countByUserId 등 추가 |
| `resources/mapper/customer/CustomerMapper.xml` | userId 매핑, findByUserIdAndBusinessId, updateUserId 추가 |
| `domain/review/controller/PublicReviewController.java` | POST 엔드포인트 제거 (GET만 유지) |
| `domain/review/service/ReviewService.java` | `createReviewByCustomer()` 메서드 추가 |

---

## 11. 프론트엔드 구현 가이드

### 고객 로그인 플로우

```
1. 매장 페이지에서 "예약하기" 또는 "리뷰 작성" 클릭
2. 로그인 안된 상태 -> 로그인 페이지로 이동
3. "카카오로 시작하기" 버튼 클릭
   -> /oauth2/authorize/kakao?loginType=customer
4. 카카오 인증 완료
5. {customer-redirect-uri}?accessToken=...&refreshToken=...&isNewUser=... 로 리다이렉트
6. isNewUser=true인 경우 -> 프로필 완성 안내 (전화번호 등록)
7. 원래 페이지로 복귀
```

### 고객 예약 플로우

```
1. 매장 상세 조회 (Public)
   GET /api/public/businesses/{slug}

2. 서비스/날짜/시간 선택 (Public)
   GET /api/public/businesses/{slug}/available-dates?month=2026-02
   GET /api/public/businesses/{slug}/available-times?date=2026-02-20&serviceId=1

3. 예약 생성 (Customer 인증 필요)
   POST /api/customer/businesses/{slug}/reservations
   - 전화번호 미등록 시 CP001 에러 -> 프로필 수정 페이지로 안내

4. 내 예약 확인
   GET /api/customer/reservations
```

### 토큰 관리

- 관리자 토큰과 고객 토큰은 별도 키로 localStorage에 저장
- `/api/customer/` 요청 시 고객 토큰 사용
- `/api/businesses/` 요청 시 관리자 토큰 사용

### 주의사항

1. **고객 예약 생성 시** `customerName`/`customerPhone` 필드가 없음 (JWT에서 자동 추출)
2. **리뷰 작성 시** `phone` 필드가 없음 (JWT의 userId로 본인 확인)
3. **비회원 예약 API**(`/api/public/...`)는 기존대로 유지되므로 비회원 예약도 가능
4. **isNewUser 플래그**를 활용하여 신규 고객에게 전화번호 등록을 안내

---

## 12. DB 마이그레이션 SQL

운영 환경 적용 시 아래 SQL을 실행해야 합니다.

```sql
-- 1. users 테이블: 마케팅 동의 컬럼
ALTER TABLE users ADD COLUMN marketing_agree CHAR(1) DEFAULT 'N';

-- 2. reservations 테이블: 로그인 고객 추적
ALTER TABLE reservations ADD COLUMN user_id BIGINT;
CREATE INDEX idx_reservations_user_id ON reservations(user_id);

-- 3. customers 테이블: 사용자 계정 연동
ALTER TABLE customers ADD COLUMN user_id BIGINT;
```

---

## 13. 향후 계획 (Phase 4 이관 항목)

| 기능 | 설명 | 우선순위 |
|------|------|----------|
| 전화번호 SMS 인증 로그인 | 카카오 미사용 고객 대안 | 중 |
| 고객 프로필 이미지 업로드 | 카카오 프로필 외 직접 업로드 | 하 |
| 비회원 예약 API Deprecated | `/api/public/` 예약/리뷰 엔드포인트 제거 | 하 |
| 카카오 비즈앱 전환 | 전화번호 자동 수집 | 중 |

---

## Swagger UI

모든 API는 Swagger UI에서 테스트 가능합니다:
```
http://localhost:8080/swagger-ui.html
```

**추가된 Tag:**
- `Public Portfolio` - 포트폴리오 조회 (고객용)
- `Customer Profile` - 고객 프로필 관리
- `Customer Booking` - 고객 예약 관리
- `Customer Review` - 고객 리뷰 작성
