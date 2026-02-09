# 04. API

REST API 엔드포인트 및 사용 가이드입니다.

## API 기본 정보

- **Base URL**: `http://localhost:8080`
- **API Docs**: `http://localhost:8080/swagger-ui.html`
- **Content-Type**: `application/json`
- **인증**: JWT Bearer Token (Authorization 헤더)

## 통합 응답 포맷

### 성공 응답

```json
{
  "success": true,
  "data": { ... },
  "timestamp": "2026-02-08T12:34:56"
}
```

### 실패 응답

```json
{
  "success": false,
  "error": {
    "code": "U001",
    "message": "사용자를 찾을 수 없습니다",
    "details": { ... }
  },
  "timestamp": "2026-02-08T12:34:56"
}
```

### 페이징 응답

```json
{
  "success": true,
  "data": {
    "content": [ ... ],
    "pageInfo": {
      "page": 1,
      "size": 20,
      "totalElements": 100,
      "totalPages": 5
    }
  },
  "timestamp": "2026-02-08T12:34:56"
}
```

## API 엔드포인트 목록

### 1. Auth (인증)

#### POST /api/auth/login
로그인

**Request**:
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response**:
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
      "email": "user@example.com",
      "name": "홍길동",
      "role": "OWNER"
    }
  }
}
```

#### POST /api/auth/register
회원가입 (사용자 + 매장 동시 생성)

**Request**:
```json
{
  "email": "owner@example.com",
  "password": "password123",
  "name": "홍길동",
  "phone": "010-1234-5678",
  "businessName": "홍길동 미용실",
  "businessType": "BEAUTY_SHOP",
  "address": "서울시 강남구 역삼동 123",
  "phone": "02-1234-5678"
}
```

#### POST /api/auth/refresh
Access Token 갱신

**Request**:
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

#### POST /api/auth/logout
로그아웃

**Request**:
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### 2. User (사용자)

#### GET /api/users
사용자 목록 조회

**Query Parameters**:
- `businessId` (optional): 매장 ID
- `role` (optional): ADMIN, OWNER, STAFF
- `status` (optional): ACTIVE, INACTIVE, SUSPENDED
- `keyword` (optional): 이름/이메일 검색
- `page` (default: 1): 페이지 번호
- `size` (default: 20): 페이지 크기

**Response**:
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "email": "user@example.com",
        "name": "홍길동",
        "role": "OWNER",
        "status": "ACTIVE",
        "createdAt": "2026-01-01T00:00:00"
      }
    ],
    "pageInfo": {
      "page": 1,
      "size": 20,
      "totalElements": 1,
      "totalPages": 1
    }
  }
}
```

#### GET /api/users/{id}
사용자 상세 조회

#### POST /api/users
사용자 생성

#### PUT /api/users/{id}
사용자 수정

#### DELETE /api/users/{id}
사용자 삭제

### 3. Business (매장)

#### GET /api/businesses
매장 목록 조회

#### GET /api/businesses/{id}
매장 상세 조회

#### POST /api/businesses
매장 생성

#### PUT /api/businesses/{id}
매장 수정

#### DELETE /api/businesses/{id}
매장 삭제

### 4. Staff (직원)

#### GET /api/businesses/{businessId}/staffs
직원 목록 조회

#### POST /api/businesses/{businessId}/staffs
직원 생성

**Request**:
```json
{
  "name": "김디자이너",
  "specialty": "컷 전문",
  "careerYears": 5,
  "introduction": "10년 경력 스타일리스트",
  "isActive": true
}
```

#### GET /api/businesses/{businessId}/staffs/{id}
직원 상세 조회

#### PUT /api/businesses/{businessId}/staffs/{id}
직원 수정

#### DELETE /api/businesses/{businessId}/staffs/{id}
직원 삭제

### 5. Portfolio (포트폴리오)

#### GET /api/staffs/{staffId}/portfolios
포트폴리오 목록 조회

#### POST /api/staffs/{staffId}/portfolios
포트폴리오 생성

**Request**:
```json
{
  "imageUrl": "https://example.com/image.jpg",
  "description": "2024년 트렌드 컷",
  "tags": "컷,염색,펌"
}
```

#### DELETE /api/staffs/{staffId}/portfolios/{id}
포트폴리오 삭제

### 6. Service (서비스 메뉴)

#### GET /api/businesses/{businessId}/services
서비스 목록 조회

#### POST /api/businesses/{businessId}/services
서비스 생성

**Request**:
```json
{
  "name": "컷",
  "description": "기본 커트",
  "duration": 60,
  "price": 30000,
  "staffIds": [1, 2, 3]
}
```

#### GET /api/businesses/{businessId}/services/{id}
서비스 상세 조회

#### PUT /api/businesses/{businessId}/services/{id}
서비스 수정

#### DELETE /api/businesses/{businessId}/services/{id}
서비스 삭제

### 7. Customer (고객)

#### GET /api/businesses/{businessId}/customers
고객 목록 조회

**Query Parameters**:
- `keyword`: 이름/전화번호 검색
- `tags`: 태그 필터
- `minVisitCount`: 최소 방문 횟수
- `page`, `size`

#### POST /api/businesses/{businessId}/customers
고객 생성

**Request**:
```json
{
  "name": "김고객",
  "phone": "010-1234-5678",
  "email": "customer@example.com",
  "birthday": "1990-01-01",
  "gender": "MALE",
  "tags": "VIP,단골"
}
```

#### GET /api/businesses/{businessId}/customers/{id}
고객 상세 조회

#### PUT /api/businesses/{businessId}/customers/{id}
고객 수정

#### DELETE /api/businesses/{businessId}/customers/{id}
고객 삭제

### 8. Customer History (고객 이력)

#### GET /api/customers/{customerId}/histories
고객 이력 목록 조회

#### POST /api/customers/{customerId}/histories
고객 이력 생성

**Request**:
```json
{
  "staffId": 1,
  "serviceIds": [1, 2],
  "visitDate": "2026-02-08",
  "totalPrice": 110000,
  "details": {
    "hairLength": "short",
    "hairType": "straight",
    "dyeColor": "brown"
  },
  "notes": "다음에는 더 짧게"
}
```

### 9. Reservation (예약)

#### POST /api/businesses/{businessId}/reservations
예약 생성

**Request**:
```json
{
  "customerId": 1,
  "customerName": "김고객",
  "customerPhone": "010-1234-5678",
  "staffId": 1,
  "reservationDate": "2026-02-10",
  "startTime": "14:00",
  "serviceIds": [1, 2],
  "customerMemo": "빨리 부탁드려요"
}
```

**Response**:
```json
{
  "success": true,
  "data": {
    "id": 123,
    "reservationNumber": "260210-A3B9",
    "reservationDate": "2026-02-10",
    "startTime": "14:00",
    "endTime": "16:00",
    "totalDuration": 120,
    "totalPrice": 110000,
    "status": "PENDING",
    "customer": {
      "id": 1,
      "name": "김고객",
      "phone": "010-1234-5678"
    },
    "staff": {
      "id": 1,
      "name": "김디자이너",
      "specialty": "컷 전문"
    },
    "services": [
      {"id": 1, "name": "컷", "price": 30000, "duration": 60},
      {"id": 2, "name": "펌", "price": 80000, "duration": 60}
    ],
    "createdAt": "2026-02-08T12:34:56"
  }
}
```

#### GET /api/businesses/{businessId}/reservations
예약 목록 조회

**Query Parameters**:
- `date`: 특정 날짜 (YYYY-MM-DD)
- `startDate`, `endDate`: 날짜 범위
- `staffId`: 직원 필터
- `customerId`: 고객 필터
- `status`: PENDING, CONFIRMED, COMPLETED, CANCELLED, NO_SHOW
- `page`, `size`

#### GET /api/businesses/{businessId}/reservations/{id}
예약 상세 조회

#### PUT /api/businesses/{businessId}/reservations/{id}
예약 수정

#### PATCH /api/businesses/{businessId}/reservations/{id}/confirm
예약 확정

#### PATCH /api/businesses/{businessId}/reservations/{id}/complete
예약 완료

#### PATCH /api/businesses/{businessId}/reservations/{id}/cancel
예약 취소

#### DELETE /api/businesses/{businessId}/reservations/{id}
예약 삭제

### 10. Special Holiday (특별 휴무일)

#### GET /api/businesses/{businessId}/holidays
휴무일 목록 조회

#### POST /api/businesses/{businessId}/holidays
휴무일 생성

**Request**:
```json
{
  "holidayDate": "2026-02-15",
  "reason": "설날"
}
```

#### DELETE /api/businesses/{businessId}/holidays/{id}
휴무일 삭제

### 11. Dashboard (대시보드)

#### GET /api/businesses/{businessId}/dashboard
대시보드 조회

**Query Parameters**:
- `date` (optional): 기준 날짜 (YYYY-MM-DD, default: today)

**Response**:
```json
{
  "success": true,
  "data": {
    "todayStats": {
      "reservationCount": 12,
      "completedCount": 8,
      "cancelledCount": 1,
      "totalRevenue": 480000
    },
    "weekStats": {
      "reservationCount": 45,
      "completedCount": 38,
      "cancelledCount": 3,
      "totalRevenue": 1920000
    },
    "monthStats": {
      "reservationCount": 180,
      "completedCount": 160,
      "cancelledCount": 10,
      "totalRevenue": 8400000,
      "newCustomerCount": 25
    },
    "dailyCounts": [
      {"date": "2026-02-01", "count": 8},
      {"date": "2026-02-02", "count": 10},
      ...
    ],
    "recentReservations": [
      {
        "id": 123,
        "customerName": "김고객",
        "startTime": "14:00",
        "status": "CONFIRMED"
      }
    ],
    "recentCustomers": [
      {
        "id": 456,
        "name": "이고객",
        "visitCount": 3,
        "totalSpent": 120000
      }
    ]
  }
}
```

## 인증 방식

### JWT Bearer Token

모든 API 요청 시 Authorization 헤더에 JWT 토큰을 포함해야 합니다 (Public 엔드포인트 제외).

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Public 엔드포인트 (인증 불필요)

- `POST /api/auth/login`
- `POST /api/auth/register`
- `POST /api/auth/refresh`
- `GET /api/health`
- `/swagger-ui/**`

## 에러 코드

| 코드 | 메시지 | HTTP 상태 |
|------|--------|----------|
| **C001** | 잘못된 입력값입니다 | 400 |
| **C003** | 서버 내부 오류가 발생했습니다 | 500 |
| **A001** | 인증이 필요합니다 | 401 |
| **A002** | 유효하지 않은 토큰입니다 | 401 |
| **A004** | 아이디 또는 비밀번호가 올바르지 않습니다 | 401 |
| **U001** | 사용자를 찾을 수 없습니다 | 404 |
| **U002** | 이미 사용 중인 이메일입니다 | 409 |
| **B001** | 매장을 찾을 수 없습니다 | 404 |
| **B003** | 해당 매장에 접근 권한이 없습니다 | 403 |
| **R001** | 예약을 찾을 수 없습니다 | 404 |
| **R002** | 이미 예약된 시간입니다 | 409 |
| **R006** | 해당 날짜는 휴무일입니다 | 400 |

전체 에러 코드는 [예외 처리 문서](../01_architecture/exception-handling.md) 참고.

## Swagger UI

개발 환경에서 Swagger UI를 통해 API를 테스트할 수 있습니다.

**URL**: http://localhost:8080/swagger-ui.html

### 인증 방법

1. `/api/auth/login` 엔드포인트로 로그인
2. 응답에서 `accessToken` 복사
3. Swagger UI 우측 상단 "Authorize" 버튼 클릭
4. `Bearer {accessToken}` 입력
5. "Authorize" 버튼 클릭

## 관련 문서

- [도메인 가이드](../02_domain/README.md)
- [보안 구조](../01_architecture/security.md)
- [예외 처리](../01_architecture/exception-handling.md)
