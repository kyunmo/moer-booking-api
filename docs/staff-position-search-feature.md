# 스태프 직급 관리 + 검색 기능

## 개요
매장별 직급 체계를 관리하고, 직원을 다양한 조건으로 검색할 수 있는 기능.

---

## 1. DB 변경사항

### 신규 테이블: `staff_positions`
```sql
CREATE TABLE staff_positions (
    id BIGSERIAL PRIMARY KEY,
    business_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(200),
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```
- 인덱스: `idx_staff_positions_business_id`, `idx_staff_positions_business_name` (UNIQUE)

### 기존 테이블 변경: `staffs`
- `position_id BIGINT` 컬럼 추가 (staff_positions 참조)
- 인덱스: `idx_staffs_position_id`
- 기존 `position VARCHAR(50)` 유지 (하위 호환)

---

## 2. 직급 관리 API

### 에러 코드
| 코드 | 이름 | HTTP | 설명 |
|------|------|------|------|
| SP001 | STAFF_POSITION_NOT_FOUND | 404 | 직급을 찾을 수 없습니다 |
| SP002 | STAFF_POSITION_DUPLICATE_NAME | 409 | 이미 존재하는 직급명입니다 |
| SP003 | STAFF_POSITION_HAS_STAFFS | 400 | 해당 직급에 직원이 존재하여 삭제할 수 없습니다 |
| SP004 | STAFF_POSITION_SORT_ORDER_INVALID | 400 | 정렬 순서가 올바르지 않습니다 |

### 엔드포인트

#### POST `/api/businesses/{businessId}/staff-positions`
직급 생성. sortOrder 자동 설정 (max + 1).

**Request Body:**
```json
{
  "name": "원장",
  "description": "매장 총괄"
}
```

**Response (201):**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "businessId": 1,
    "name": "원장",
    "description": "매장 총괄",
    "sortOrder": 1,
    "staffCount": null,
    "createdAt": "2026-02-14 10:00:00",
    "updatedAt": "2026-02-14 10:00:00"
  }
}
```

#### GET `/api/businesses/{businessId}/staff-positions`
직급 목록 조회 (sort_order 순, staffCount 포함).

#### GET `/api/businesses/{businessId}/staff-positions/{positionId}`
직급 단건 조회 (staffCount 포함).

#### PATCH `/api/businesses/{businessId}/staff-positions/{positionId}`
직급 수정 (부분 업데이트).

**Request Body:**
```json
{
  "name": "수석 디자이너",
  "description": "5년차 이상"
}
```

#### DELETE `/api/businesses/{businessId}/staff-positions/{positionId}`
직급 삭제. 해당 직급에 직원이 배정되어 있으면 삭제 불가 (SP003).

#### PATCH `/api/businesses/{businessId}/staff-positions/sort-order`
정렬 순서 일괄 변경.

**Request Body:**
```json
{
  "items": [
    { "id": 1, "sortOrder": 1 },
    { "id": 2, "sortOrder": 2 },
    { "id": 3, "sortOrder": 3 }
  ]
}
```

---

## 3. Staff ↔ Position 연동

### 변경된 필드
- `StaffCreateRequest`: `positionId` (선택) 추가
- `StaffUpdateRequest`: `positionId` (선택) 추가
- `StaffResponse`: `positionId`, `positionName` 추가

### 동작 방식
1. `positionId` 설정 시 → 해당 직급의 존재 및 매장 소속 검증
2. 검증 통과 시 → `position` 텍스트를 직급명으로 자동 채움
3. `positionId` 없이 `position` 텍스트만 설정 → 기존 방식 동일
4. 응답 시 `positionId`가 있으면 `positionName` 자동 매핑

---

## 4. 스태프 검색 기능

### GET `/api/businesses/{businessId}/staffs` 확장

기존 파라미터:
- `activeOnly` (boolean, default: false)

추가 파라미터:
| 파라미터 | 타입 | 검색 방식 | 설명 |
|---------|------|----------|------|
| `name` | String | LIKE | 이름 검색 |
| `positionId` | Long | exact | 직급 ID 필터 |
| `specialty` | String | LIKE | 전문분야 검색 |
| `minCareerYears` | Integer | >= | 최소 경력 필터 |
| `sortBy` | String | - | 정렬 기준 (name, position, career_years, created_at) |
| `sortOrder` | String | - | 정렬 방향 (asc, desc) |

### 예시
```
# 이름으로 검색
GET /api/businesses/1/staffs?name=김

# 특정 직급 + 경력 3년 이상
GET /api/businesses/1/staffs?positionId=2&minCareerYears=3

# 경력순 정렬
GET /api/businesses/1/staffs?sortBy=career_years&sortOrder=desc

# 활성 직원 중 전문분야 검색
GET /api/businesses/1/staffs?activeOnly=true&specialty=컬러
```

### 하위 호환성
- 필터 파라미터 없이 호출 시 기존 동작과 완전 동일

---

## 5. 파일 구조

### 신규 파일
```
domain/staff/position/
├── StaffPosition.java
├── controller/
│   └── StaffPositionController.java
├── dto/
│   ├── StaffPositionCreateRequest.java
│   ├── StaffPositionUpdateRequest.java
│   └── StaffPositionResponse.java
├── repository/
│   └── StaffPositionRepository.java
└── service/
    └── StaffPositionService.java

domain/staff/dto/
└── StaffSearchCondition.java

resources/mapper/staff/
└── StaffPositionMapper.xml
```

### 수정된 파일
- `schema.sql` - staff_positions 테이블, staffs.position_id 추가
- `ErrorCode.java` - SP001~SP004 추가
- `Staff.java` - positionId 필드 추가
- `StaffCreateRequest.java` - positionId 필드 추가
- `StaffUpdateRequest.java` - positionId 필드 추가
- `StaffResponse.java` - positionId, positionName 필드 및 from() 오버로드
- `StaffMapper.xml` - position_id 매핑, 동적 검색 쿼리 추가
- `StaffRepository.java` - findByCondition, countByCondition 추가
- `StaffService.java` - StaffPositionRepository 주입, searchStaffs(), toResponse() 추가
- `StaffController.java` - GET /staffs 검색 파라미터 확장
