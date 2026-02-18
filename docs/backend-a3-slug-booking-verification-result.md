# A-3. 슬러그(slug) 예약 페이지 E2E 검증 - 백엔드 확인 결과

> **작성일:** 2026-02-18
> **상태:** 백엔드 구현 완료 + 스키마/로직 보완 완료

---

## 1. 발견된 이슈 및 수정 내역

### 이슈 1: schema.sql에 slug 컬럼 누락 (CRITICAL)

**문제:** `businesses` 테이블에 `slug` 컬럼이 schema.sql에 정의되지 않았음. Entity, DTO, Mapper XML 모두 slug를 사용하지만 DDL에 누락.

**수정:** `schema.sql` businesses 테이블에 다음 컬럼들 추가:
```sql
slug VARCHAR(50) UNIQUE,
profile_image_url TEXT,
gallery_images JSONB,
latitude DOUBLE PRECISION,
longitude DOUBLE PRECISION,
tags TEXT,
average_rating DOUBLE PRECISION,
review_count INTEGER DEFAULT 0,
```

**마이그레이션 SQL (이미 운영 중인 DB):**
```sql
-- 이미 수동으로 추가된 경우 무시
ALTER TABLE businesses ADD COLUMN IF NOT EXISTS slug VARCHAR(50) UNIQUE;
ALTER TABLE businesses ADD COLUMN IF NOT EXISTS profile_image_url TEXT;
ALTER TABLE businesses ADD COLUMN IF NOT EXISTS gallery_images JSONB;
ALTER TABLE businesses ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION;
ALTER TABLE businesses ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION;
ALTER TABLE businesses ADD COLUMN IF NOT EXISTS tags TEXT;
ALTER TABLE businesses ADD COLUMN IF NOT EXISTS average_rating DOUBLE PRECISION;
ALTER TABLE businesses ADD COLUMN IF NOT EXISTS review_count INTEGER DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_businesses_slug ON businesses(slug);
```

---

### 이슈 2: 매장 생성 시 slug 미설정

**문제:** `BusinessService.createBusiness()`에서 slug를 설정하지 않아, 새로 생성된 매장의 slug가 null.

**수정:** 매장 생성 시 자동으로 임시 slug 생성 (`biz-{uuid8}` 형식)
- 예: `biz-a1b2c3d4`
- 사용자가 매장 설정에서 원하는 slug로 변경 가능

---

### 이슈 3: updateBusiness에서 slug 누락

**문제:** `BusinessService.updateBusiness()`의 builder에서 slug를 설정하지 않아, 매장 수정 시 slug가 null로 초기화됨.

**수정:** `business.getSlug()`로 기존 slug 보존하도록 수정.

---

## 2. API 현황 (모두 구현 완료)

### 2.1 매장 상세에 slug 포함 확인

**GET /api/businesses/{businessId}** (관리자용, 인증 필요)

```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "마이살롱",
    "slug": "biz-a1b2c3d4",  // ✅ slug 포함
    "businessType": "BEAUTY_SHOP",
    ...
  }
}
```

---

### 2.2 Public API (인증 불필요)

#### GET /api/public/businesses/{slug} - 슬러그로 매장 상세 조회

**성공 응답:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "slug": "my-salon",
    "name": "마이살롱",
    "businessType": "BEAUTY_SHOP",
    "description": "...",
    "address": "...",
    "phone": "...",
    "profileImageUrl": "...",
    "galleryImages": [...],
    "averageRating": 4.5,
    "reviewCount": 25,
    "businessHours": {...},
    "tags": "예약가능,주차가능",
    "services": [
      {
        "id": 1,
        "name": "여성 커트",
        "duration": 60,
        "price": 25000,
        "categoryName": "커트"
      }
    ],
    "staffs": [
      {
        "id": 1,
        "name": "김디자이너",
        "position": "원장",
        "profileImageUrl": "...",
        "portfolioCount": 5
      }
    ]
  }
}
```

**에러 응답:**

| 상황 | HTTP 상태 | 에러 코드 | 메시지 |
|------|----------|----------|--------|
| 슬러그 미존재 | 404 | `B001` | 매장을 찾을 수 없습니다 |
| 비활성 매장 | 404 | `B001` | 매장을 찾을 수 없습니다 |

> 비활성 매장도 404로 응답하여 존재 여부를 노출하지 않습니다.

---

#### GET /api/public/businesses/check-slug?slug={slug} - 슬러그 사용 가능 확인

**사용 가능:**
```json
{
  "success": true,
  "data": {
    "available": true,
    "suggestions": null
  }
}
```

**사용 불가 (중복/예약어/형식 오류):**
```json
{
  "success": true,
  "data": {
    "available": false,
    "suggestions": ["my-salon-1", "my-salon-2", "my-salon-3"]
  }
}
```

**슬러그 규칙:**
- 길이: 3~50자
- 허용 문자: 소문자(a-z), 숫자(0-9), 하이픈(-)
- 시작/끝: 소문자 또는 숫자만
- 예약어 불가: admin, api, public, login, signup, booking, dashboard, settings, help, support, about, contact, terms, privacy

---

#### GET /api/public/businesses/{slug}/available-dates - 예약 가능 날짜

**요청 파라미터:**
| 파라미터 | 필수 | 설명 |
|---------|------|------|
| month | O | 조회 년월 (예: 2026-02) |
| staffId | X | 스태프 ID 필터 |
| serviceId | X | 서비스 ID 필터 |

---

#### GET /api/public/businesses/{slug}/available-times - 예약 가능 시간

**요청 파라미터:**
| 파라미터 | 필수 | 설명 |
|---------|------|------|
| date | O | 예약 날짜 (예: 2026-02-20) |
| serviceId | O | 서비스 ID |
| staffId | X | 스태프 ID |

---

#### POST /api/public/businesses/{slug}/reservations - 고객 예약 생성

**요청:**
```json
{
  "customerName": "홍길동",
  "customerPhone": "010-1234-5678",
  "staffId": 1,
  "serviceIds": [1, 2],
  "reservationDate": "2026-02-20",
  "startTime": "14:00",
  "customerMemo": "펌 상담 원합니다"
}
```

---

#### GET /api/public/reservations/{reservationNumber}?phone={phone} - 예약 조회

**요청 파라미터:**
| 파라미터 | 필수 | 설명 |
|---------|------|------|
| reservationNumber | O | 예약번호 (경로 파라미터) |
| phone | O | 본인 확인용 전화번호 |

---

## 3. 슬러그 에러 코드

| 에러 코드 | HTTP 상태 | 메시지 |
|----------|----------|--------|
| `BS001` | 409 | 이미 사용 중인 슬러그입니다 |
| `BS002` | 400 | 슬러그 형식이 올바르지 않습니다 |
| `BS003` | 400 | 사용할 수 없는 슬러그입니다 |

---

## 4. 프론트엔드 검증 체크리스트

### 슬러그 설정/관리
- [ ] 매장 설정에서 slug 변경 → 저장 → 조회 시 반영 확인
- [ ] slug 중복 시 대안 제안 표시
- [ ] slug 형식 오류 시 에러 메시지 표시
- [ ] 예약어 사용 시 에러 메시지 표시

### 예약 전체 플로우 (E2E)
- [ ] `/booking/{slug}` 접근 → 매장 상세 정보 표시
- [ ] 서비스 선택 → 날짜/시간 선택 → 예약 정보 입력
- [ ] 예약 완료 → 예약번호 표시
- [ ] 비회원: 예약번호 + 전화번호로 조회
- [ ] 회원(카카오 로그인): 내 예약 목록에서 확인

### 엣지 케이스
- [ ] slug 미설정 매장 → 자동 생성된 `biz-{uuid8}` slug로 접근 가능
- [ ] 존재하지 않는 slug → 404 에러 페이지
- [ ] 비활성(INACTIVE) 매장 slug → 404 에러 페이지
- [ ] SEO 메타태그 (og:title, og:description, og:image)

---

## 5. 변경 이력

| 날짜 | 변경 내용 | 파일 |
|------|----------|------|
| 2026-02-18 | schema.sql에 slug 외 누락 컬럼 7개 추가 | `db/schema.sql` |
| 2026-02-18 | 매장 생성 시 slug 자동 생성 (`biz-{uuid8}`) | `BusinessService.java` |
| 2026-02-18 | updateBusiness에서 slug 보존 누락 수정 | `BusinessService.java` |
