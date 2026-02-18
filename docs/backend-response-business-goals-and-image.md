# 백엔드 응답: 매장 목표 설정 필드 및 프로필 이미지 API

> **작성일:** 2026-02-18
> **상태:** 구현 완료

---

## 1. 매장 목표 설정 필드 - 수정 완료

### 발견된 이슈

**문제:** DB, Entity, Mapper, Service에는 매출목표 3개 필드가 모두 존재했으나,
**`BusinessResponse` DTO에서 누락**되어 API 응답에 포함되지 않았음.

| 레이어 | 상태 (수정 전) |
|--------|-------------|
| DB Schema (`businesses` 테이블) | ✅ 존재 |
| Entity (`Business.java`) | ✅ 존재 |
| UpdateRequest DTO | ✅ 존재 |
| Mapper XML (ResultMap + UPDATE) | ✅ 존재 |
| Service (`updateBusiness()`) | ✅ 존재 |
| **Response DTO** | **❌ 누락** |

### 수정 내역

**파일:** `BusinessResponse.java`

1. 필드 4개 추가:
```java
private String profileImageUrl;
private Integer dailyRevenueGoal;
private Integer monthlyRevenueGoal;
private Integer monthlyNewCustomerGoal;
```

2. `from(Business)` 메서드 수정 - 4개 필드 매핑 추가
3. `from(Business, BusinessSettings)` 메서드 수정 - 4개 필드 매핑 추가

### 수정 후 응답 예시

**GET /api/businesses/{businessId}**

```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "마이살롱",
    "businessType": "BEAUTY_SHOP",
    "slug": "my-salon",
    "profileImageUrl": "/uploads/businesses/abc123.jpg",
    "dailyRevenueGoal": 500000,
    "monthlyRevenueGoal": 15000000,
    "monthlyNewCustomerGoal": 50,
    "phone": "02-1234-5678",
    "address": "서울시 강남구...",
    "description": "...",
    "businessHours": {...},
    "status": "ACTIVE",
    "settings": {...},
    "createdAt": "2026-01-15 10:00:00",
    "updatedAt": "2026-02-18 14:30:00"
  }
}
```

### 매출목표 수정 (기존 API 활용)

**PATCH /api/businesses/{businessId}**

```json
{
  "dailyRevenueGoal": 500000,
  "monthlyRevenueGoal": 15000000,
  "monthlyNewCustomerGoal": 50
}
```

> 이 API는 이미 정상 동작하고 있었음. Response만 누락이었음.

---

## 2. 매장 프로필 이미지 업로드/삭제 API - 신규 구현

### POST /api/businesses/{businessId}/profile-image

- **설명:** 매장 프로필 이미지 업로드
- **Content-Type:** `multipart/form-data`
- **인증:** 필수 (JWT Bearer Token)
- **권한:** 해당 매장 접근 권한 필요

**요청 파라미터:**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `image` | File | YES | 이미지 파일 |

**제약 조건:**
- 최대 파일 크기: 5MB
- 허용 확장자: jpg, jpeg, png, webp

**성공 응답 (200):**

```json
{
  "success": true,
  "data": {
    "profileImageUrl": "/uploads/businesses/a1b2c3d4-e5f6.jpg"
  }
}
```

**에러 응답:**

| HTTP 코드 | 에러 코드 | 설명 |
|-----------|----------|------|
| 400 | `FI002` | 파일 크기 초과 (5MB) |
| 400 | `FI003` | 지원하지 않는 파일 형식 |
| 403 | `B002` | 매장 접근 권한 없음 |
| 404 | `B001` | 매장 미존재 |

**동작:**
- 기존 이미지가 있으면 자동 삭제 후 새 이미지 저장 (덮어쓰기)
- 파일은 `/uploads/businesses/` 디렉토리에 UUID 파일명으로 저장

---

### DELETE /api/businesses/{businessId}/profile-image

- **설명:** 매장 프로필 이미지 삭제
- **인증:** 필수 (JWT Bearer Token)
- **권한:** 해당 매장 접근 권한 필요

**성공 응답 (200):**

```json
{
  "success": true,
  "data": null
}
```

**에러 응답:**

| HTTP 코드 | 에러 코드 | 설명 |
|-----------|----------|------|
| 403 | `B002` | 매장 접근 권한 없음 |
| 404 | `B001` | 매장 미존재 |

---

## 3. 마이그레이션 SQL

**매출목표 필드** - 이미 schema.sql에 존재. 운영 DB에도 추가된 상태라면 별도 작업 불필요.

```sql
-- 혹시 운영 DB에 없을 경우:
ALTER TABLE businesses ADD COLUMN IF NOT EXISTS daily_revenue_goal INTEGER;
ALTER TABLE businesses ADD COLUMN IF NOT EXISTS monthly_revenue_goal INTEGER;
ALTER TABLE businesses ADD COLUMN IF NOT EXISTS monthly_new_customer_goal INTEGER;
```

**프로필 이미지 필드** - 이전 A-3 작업에서 이미 추가됨.

```sql
-- 이전 작업에서 추가됨:
ALTER TABLE businesses ADD COLUMN IF NOT EXISTS profile_image_url TEXT;
```

---

## 4. 프론트엔드 검증 체크리스트

### 매출목표
- [ ] 매장 설정에서 일일/월간 매출목표, 월간 신규 고객 목표 입력
- [ ] PATCH /api/businesses/{id}로 저장
- [ ] GET /api/businesses/{id} 응답에서 `dailyRevenueGoal`, `monthlyRevenueGoal`, `monthlyNewCustomerGoal` 확인
- [ ] 대시보드에서 목표 대비 달성률 표시 확인

### 프로필 이미지
- [ ] 매장 설정에서 이미지 업로드 → `profileImageUrl` 반환 확인
- [ ] 이미지 교체 (기존 이미지 자동 삭제 + 새 이미지 저장)
- [ ] 이미지 삭제 → `profileImageUrl` null 확인
- [ ] 슬러그 예약 페이지에서 매장 프로필 이미지 표시 확인
- [ ] 5MB 초과 파일 업로드 시 에러 처리 확인
- [ ] 지원하지 않는 형식 (.gif, .bmp 등) 업로드 시 에러 처리 확인

---

## 5. 변경 파일 목록

| 파일 | 변경 내용 |
|------|----------|
| `BusinessResponse.java` | `profileImageUrl`, `dailyRevenueGoal`, `monthlyRevenueGoal`, `monthlyNewCustomerGoal` 필드 및 from() 매핑 추가 |
| `BusinessController.java` | `POST /{id}/profile-image`, `DELETE /{id}/profile-image` 엔드포인트 추가 |
| `BusinessService.java` | `uploadProfileImage()`, `deleteProfileImage()` 메서드 추가 |
| `BusinessRepository.java` | `updateProfileImageUrl()` 메서드 추가 |
| `BusinessMapper.xml` | `updateProfileImageUrl` SQL 추가 |
