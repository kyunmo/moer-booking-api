# Phase 3: 백엔드 API 응답서 — 고객용 예약 시스템 (B2C)

> 작성일: 2026-02-15
> 요청 문서: `docs/phase3-backend-request.md`
> 상태: **구현 완료**

---

## 아키텍처 결정 사항

| 항목 | 결정 | 근거 |
|------|------|------|
| 고객 계정 체계 | **비회원 예약** (전화번호+예약번호 인증) | MVP 단계에서 불필요한 회원가입 장벽 제거 |
| 예약 확정 방식 | **매장 설정(`auto_confirm`) 기반** | 기존 BusinessSettings의 설정 재사용 |
| 슬러그 생성 | **자동 생성(`b-{id}`)** + 수동 변경 가능 | 매장 등록 시 자동 생성, Admin이 변경 가능 |
| 리뷰 노출 정책 | **즉시 노출** + Admin 삭제 가능 | 고객 UX 우선, 부적절한 리뷰는 Admin 관리 |
| 카카오톡 알림 | **인프라만 구현** (LogNotificationSender) | 실제 카카오 연동은 외부 채널 등록 후 교체 |
| 인증 방식 | `/api/public/**` = **인증 불필요** | SecurityConfig에 permitAll 적용 완료 |

---

## 1. 매장 슬러그 시스템

### 1-1. 슬러그 사용 가능 여부 확인

```
GET /api/public/businesses/check-slug?slug={slug}
인증: 불필요
```

**Query Parameters:**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `slug` | String | O | 확인할 슬러그 (영문 소문자, 숫자, 하이픈) |

**Response 200:**
```json
{
  "status": "success",
  "data": {
    "available": true,
    "suggestions": null
  }
}
```

**사용 불가 시:**
```json
{
  "status": "success",
  "data": {
    "available": false,
    "suggestions": ["miso-hair-1", "miso-hair-2", "miso-hair-3"]
  }
}
```

### 1-2. 슬러그 변경 (Admin)

```
PATCH /api/businesses/{businessId}/slug
인증: Bearer Token (Admin)
```

**Request Body:**
```json
{
  "slug": "miso-hair"
}
```

**Validation:**
- 3~50자
- 영문 소문자, 숫자, 하이픈만 가능
- 시작/끝은 소문자 또는 숫자
- 정규식: `^[a-z0-9][a-z0-9-]*[a-z0-9]$`

**Response 200:**
```json
{
  "status": "success",
  "message": "슬러그가 변경되었습니다."
}
```

**Error Codes:**
| 코드 | HTTP | 설명 |
|------|------|------|
| BS001 | 409 | 이미 사용 중인 슬러그 |
| BS002 | 400 | 슬러그 형식 오류 |
| BS003 | 400 | 예약어(admin, api 등) 사용 불가 |

---

## 2. 매장 검색/상세 조회 (Public)

### 2-1. 매장 검색/목록

```
GET /api/public/businesses
인증: 불필요
```

**Query Parameters:**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| `keyword` | String | X | - | 매장명/주소 검색 |
| `businessType` | String | X | - | 업종 필터 (BEAUTY_SHOP, PILATES 등) |
| `sortBy` | String | X | `rating` | 정렬 기준: `rating`, `name`, `created_at` |
| `page` | Integer | X | 1 | 페이지 번호 (1부터) |
| `size` | Integer | X | 20 | 페이지 크기 |

**Response 200:**
```json
{
  "status": "success",
  "data": {
    "content": [
      {
        "id": 1,
        "slug": "miso-hair",
        "name": "미소 헤어",
        "businessType": "BEAUTY_SHOP",
        "address": "서울시 강남구 역삼동 123-45",
        "phone": "02-1234-5678",
        "profileImageUrl": "/uploads/businesses/1_profile.jpg",
        "averageRating": 4.5,
        "reviewCount": 128,
        "todayHours": "10:00 - 20:00",
        "open": true,
        "tags": ["예약가능", "주차가능", "카드결제"],
        "createdAt": "2026-01-15 09:00:00"
      }
    ],
    "pageInfo": {
      "page": 1,
      "size": 20,
      "totalElements": 45,
      "totalPages": 3
    }
  }
}
```

**응답 필드 설명:**
- `todayHours`: 오늘 요일의 영업시간 (예: `"10:00 - 20:00"`, 휴무 시 `"휴무"`)
- `open`: 현재 영업 중 여부 (영업시간 내이면 `true`)
- `tags`: 쉼표 구분 태그 문자열을 배열로 파싱
- `averageRating`: 활성 리뷰 기준 평균 평점
- `reviewCount`: 활성 리뷰 총 수

### 2-2. 매장 상세 조회

```
GET /api/public/businesses/{slug}
인증: 불필요
```

**Response 200:**
```json
{
  "status": "success",
  "data": {
    "id": 1,
    "slug": "miso-hair",
    "name": "미소 헤어",
    "businessType": "BEAUTY_SHOP",
    "description": "강남 역삼동 프리미엄 헤어살롱",
    "address": "서울시 강남구 역삼동 123-45",
    "phone": "02-1234-5678",
    "profileImageUrl": "/uploads/businesses/1_profile.jpg",
    "galleryImages": [
      "/uploads/businesses/1_gallery_01.jpg",
      "/uploads/businesses/1_gallery_02.jpg"
    ],
    "averageRating": 4.5,
    "reviewCount": 128,
    "businessHours": {
      "mon": { "open": "09:00", "close": "20:00" },
      "tue": { "open": "09:00", "close": "20:00" },
      "wed": { "open": "09:00", "close": "20:00" },
      "thu": { "open": "09:00", "close": "20:00" },
      "fri": { "open": "09:00", "close": "21:00" },
      "sat": { "open": "10:00", "close": "18:00" },
      "sun": null
    },
    "tags": ["예약가능", "주차가능"],
    "services": [
      {
        "id": 1,
        "categoryName": "커트",
        "name": "여성컷",
        "description": "샴푸 + 커트 + 드라이",
        "price": 30000,
        "duration": 60
      },
      {
        "id": 2,
        "categoryName": "펌",
        "name": "디지털펌",
        "description": "디지털펌 + 트리트먼트",
        "price": 120000,
        "duration": 150
      }
    ],
    "staffs": [
      {
        "id": 1,
        "name": "김디자이너",
        "position": "원장",
        "profileImageUrl": "/uploads/staffs/1_profile.jpg",
        "introduction": "15년 경력 헤어 디자이너",
        "specialty": "여성컷, 펌, 염색",
        "portfolioCount": 23
      }
    ],
    "createdAt": "2026-01-15 09:00:00"
  }
}
```

**참고:** `businessHours`의 요일 키는 `mon`, `tue`, `wed`, `thu`, `fri`, `sat`, `sun`이며, `null`이면 휴무입니다.

---

## 3. 예약 API (Public)

### 3-1. 예약 가능 날짜 조회

```
GET /api/public/businesses/{slug}/available-dates
인증: 불필요
```

**Query Parameters:**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `month` | String | O | 조회 년월 (예: `2026-02`) |
| `staffId` | Long | X | 특정 스태프 필터 |
| `serviceId` | Long | X | 특정 서비스 필터 |

**Response 200:**
```json
{
  "status": "success",
  "data": {
    "month": "2026-02",
    "availableDates": [
      { "date": "2026-02-15", "hasSlots": true },
      { "date": "2026-02-16", "hasSlots": true },
      { "date": "2026-02-17", "hasSlots": false },
      { "date": "2026-02-18", "hasSlots": true }
    ]
  }
}
```

**hasSlots 판단 로직:**
- 영업일인지 확인 (businessHours에 오늘 요일이 존재)
- 특별 휴무일이 아닌지 확인 (holidays 테이블)
- 과거 날짜는 `false`
- maxAdvanceBookingDays 범위 내인지 확인

### 3-2. 예약 가능 시간 조회

```
GET /api/public/businesses/{slug}/available-times
인증: 불필요
```

**Query Parameters:**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `date` | LocalDate | O | 조회 날짜 (예: `2026-02-20`) |
| `serviceId` | Long | O | 서비스 ID (소요시간 기반 슬롯 계산) |
| `staffId` | Long | X | 특정 스태프 지정 (미지정 시 모든 가용 스태프) |

**Response 200:**
```json
{
  "status": "success",
  "data": {
    "date": "2026-02-20",
    "serviceDuration": 60,
    "availableSlots": [
      {
        "startTime": "10:00",
        "endTime": "11:00",
        "availableStaffs": [
          { "id": 1, "name": "김디자이너" },
          { "id": 2, "name": "이디자이너" }
        ]
      },
      {
        "startTime": "11:00",
        "endTime": "12:00",
        "availableStaffs": [
          { "id": 1, "name": "김디자이너" }
        ]
      },
      {
        "startTime": "14:00",
        "endTime": "15:00",
        "availableStaffs": [
          { "id": 1, "name": "김디자이너" },
          { "id": 2, "name": "이디자이너" }
        ]
      }
    ]
  }
}
```

**시간 슬롯 계산 로직:**
1. 스태프별 근무시간 (staff_schedules) 조회
2. 휴식시간 제외
3. 기존 예약 시간 제외
4. 서비스 소요시간 단위로 슬롯 분할 (30분 간격)
5. 모든 스태프의 가용 슬롯 통합 후 시간별 그룹핑

### 3-3. 예약 생성

```
POST /api/public/businesses/{slug}/reservations
인증: 불필요
Content-Type: application/json
```

**Request Body:**
```json
{
  "serviceIds": [1],
  "staffId": 1,
  "reservationDate": "2026-02-20",
  "startTime": "14:00",
  "customerName": "박서연",
  "customerPhone": "010-9876-5432",
  "customerEmail": "soyeon@email.com",
  "customerRequest": "조용한 자리 부탁드려요"
}
```

**Validation:**

| 필드 | 규칙 |
|------|------|
| `serviceIds` | 필수, 최소 1개 |
| `staffId` | 선택 (미지정 시 자동 배정) |
| `reservationDate` | 필수, `yyyy-MM-dd` |
| `startTime` | 필수, `HH:mm` |
| `customerName` | 필수 |
| `customerPhone` | 필수, `010-XXXX-XXXX` 형식 |
| `customerEmail` | 선택 |
| `customerRequest` | 선택 |

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
    "message": "예약이 완료되었습니다. 예약번호: 260220-A3B9"
  }
}
```

**status 결정 로직:**
- `auto_confirm = true` (기본) → `CONFIRMED`
- `auto_confirm = false` → `PENDING`

**Error Codes:**
| 코드 | HTTP | 설명 |
|------|------|------|
| BK001 | 409 | 해당 시간대 예약 불가 (이미 예약됨) |
| BK002 | 400 | 영업시간 외 예약 |
| BK003 | 400 | 휴무일 예약 |
| BK004 | 400 | 해당 스태프 근무 불가 |
| BK007 | 403 | 온라인 예약 비활성화 |
| BK008 | 400 | 예약 가능 기간 초과 |
| BK009 | 400 | 최소 사전 예약 시간 미충족 |

### 3-4. 예약 조회

```
GET /api/public/reservations/{reservationNumber}?phone={phone}
인증: 불필요
```

**Query Parameters:**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `phone` | String | O | 예약 시 입력한 전화번호 (본인 확인) |

**Response 200:**
```json
{
  "status": "success",
  "data": {
    "reservationNumber": "260220-A3B9",
    "status": "CONFIRMED",
    "businessName": "미소 헤어",
    "businessAddress": "서울시 강남구 역삼동 123-45",
    "businessPhone": "02-1234-5678",
    "reservationDate": "2026-02-20",
    "startTime": "14:00",
    "endTime": "15:00",
    "staffName": "김디자이너",
    "services": ["여성컷"],
    "totalPrice": 30000,
    "totalDuration": 60,
    "canCancel": true,
    "cancelDeadline": "2026-02-19 20:00:00",
    "customerMemo": "조용한 자리 부탁드려요",
    "createdAt": "2026-02-14 10:30:00"
  }
}
```

**canCancel 판단 로직:**
- 현재 시각 < cancelDeadline (예약일 전날 기준 `cancelDeadlineHours` 적용)
- status가 `PENDING` 또는 `CONFIRMED`인 경우만

**Error Codes:**
| 코드 | HTTP | 설명 |
|------|------|------|
| BK010 | 403 | 전화번호 불일치 |

### 3-5. 예약 취소

```
POST /api/public/reservations/{reservationNumber}/cancel
인증: 불필요
Content-Type: application/json
```

**Request Body:**
```json
{
  "phone": "010-9876-5432",
  "reason": "일정 변경"
}
```

**Validation:**

| 필드 | 규칙 |
|------|------|
| `phone` | 필수, `010-XXXX-XXXX` 형식 |
| `reason` | 선택 |

**Response 200:**
```json
{
  "status": "success"
}
```

**Error Codes:**
| 코드 | HTTP | 설명 |
|------|------|------|
| BK005 | 400 | 취소 기한 초과 |
| BK006 | 400 | 이미 취소/완료된 예약 |
| BK010 | 403 | 전화번호 불일치 |

---

## 4. 리뷰 시스템

### 4-1. 리뷰 목록 조회 (Public)

```
GET /api/public/businesses/{slug}/reviews
인증: 불필요
```

**Query Parameters:**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| `page` | Integer | X | 1 | 페이지 번호 |
| `size` | Integer | X | 10 | 페이지 크기 |
| `rating` | Integer | X | - | 별점 필터 (1~5) |
| `staffId` | Long | X | - | 스태프 필터 |
| `sortBy` | String | X | `latest` | 정렬: `latest`, `rating_high`, `rating_low` |

**Response 200:**
```json
{
  "status": "success",
  "data": {
    "items": [
      {
        "id": 1,
        "customerName": "박*연",
        "rating": 5,
        "content": "정말 만족스러웠어요!",
        "serviceName": "여성컷",
        "staffName": "김디자이너",
        "images": [],
        "createdAt": "2026-02-10T14:00:00",
        "reply": {
          "content": "감사합니다! 또 방문해주세요 :)",
          "createdAt": "2026-02-11T09:00:00"
        }
      }
    ],
    "totalCount": 128,
    "stats": {
      "averageRating": 4.5,
      "totalReviews": 128,
      "unrepliedCount": 5,
      "thisMonthCount": 12,
      "ratingDistribution": {
        "1": 3,
        "2": 5,
        "3": 10,
        "4": 30,
        "5": 80
      }
    }
  }
}
```

**참고:**
- `customerName`은 자동 마스킹 (1글자: `*`, 2글자: `김*`, 3글자 이상: `김*동`)
- `reply`는 답변이 없으면 `null`
- ACTIVE 상태 리뷰만 노출

### 4-2. 리뷰 작성 (Public)

```
POST /api/public/businesses/{slug}/reviews
인증: 불필요
Content-Type: application/json
```

**Request Body:**
```json
{
  "reservationNumber": "260220-A3B9",
  "phone": "010-9876-5432",
  "rating": 5,
  "content": "정말 만족스러웠어요!",
  "staffId": 1
}
```

**Validation:**

| 필드 | 규칙 |
|------|------|
| `reservationNumber` | 필수 |
| `phone` | 필수 (본인 확인) |
| `rating` | 필수, 1~5 |
| `content` | 선택 |
| `staffId` | 선택 |

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
    "createdAt": "2026-02-15T14:00:00",
    "reply": null
  }
}
```

**비즈니스 로직:**
- 예약번호로 예약 조회 → 전화번호 일치 확인
- 예약 상태 `COMPLETED`만 리뷰 작성 가능
- 동일 예약에 중복 리뷰 불가
- 리뷰 작성 후 매장 평균 평점/리뷰 수 자동 업데이트

**Error Codes:**
| 코드 | HTTP | 설명 |
|------|------|------|
| RV001 | 400 | 예약 정보 불일치 (전화번호 불일치) |
| RV002 | 400 | 완료되지 않은 예약 |
| RV003 | 409 | 이미 리뷰 작성됨 |

### 4-3. 관리자 리뷰 목록 조회 (Admin)

```
GET /api/businesses/{businessId}/reviews
인증: Bearer Token (Admin)
```

**Query Parameters:**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| `page` | Integer | X | 1 | 페이지 번호 |
| `size` | Integer | X | 10 | 페이지 크기 |
| `status` | String | X | - | 상태 필터: `ACTIVE`, `HIDDEN`, `DELETED` |
| `rating` | Integer | X | - | 별점 필터 (1~5) |
| `staffId` | Long | X | - | 스태프 필터 |
| `startDate` | LocalDate | X | - | 검색 시작일 (`yyyy-MM-dd`) |
| `endDate` | LocalDate | X | - | 검색 종료일 (`yyyy-MM-dd`) |

**Response 200:**
```json
{
  "status": "success",
  "data": {
    "items": [
      {
        "id": 1,
        "customerName": "박서연",
        "customerPhone": "010-9876-5432",
        "reservationId": 201,
        "rating": 5,
        "content": "정말 만족스러웠어요!",
        "serviceName": "여성컷",
        "staffId": 1,
        "staffName": "김디자이너",
        "images": [],
        "isReplied": true,
        "reply": {
          "content": "감사합니다!",
          "createdAt": "2026-02-11T09:00:00"
        },
        "status": "ACTIVE",
        "deleteReason": null,
        "createdAt": "2026-02-10T14:00:00"
      }
    ],
    "totalCount": 128,
    "stats": {
      "averageRating": 4.5,
      "totalReviews": 128,
      "unrepliedCount": 5,
      "thisMonthCount": 23,
      "ratingDistribution": {
        "1": 3,
        "2": 5,
        "3": 10,
        "4": 30,
        "5": 80
      }
    }
  }
}
```

**Public vs Admin 차이점:**
| 항목 | Public | Admin |
|------|--------|-------|
| 고객명 | 마스킹 (`박*연`) | 전체 (`박서연`) |
| 전화번호 | 비노출 | `010-9876-5432` |
| 예약 ID | 비노출 | 노출 |
| 리뷰 상태 | ACTIVE만 | 전체 |
| 삭제 사유 | 비노출 | 노출 |
| 통계 정보 | 기본 통계 | 상세 통계 |

### 4-4. 리뷰 답변 등록 (Admin)

```
POST /api/businesses/{businessId}/reviews/{reviewId}/reply
인증: Bearer Token (Admin)
Content-Type: application/json
```

**Request Body:**
```json
{
  "content": "감사합니다! 또 방문해주세요 :)"
}
```

**Response 200:**
```json
{
  "status": "success"
}
```

**Error Codes:**
| 코드 | HTTP | 설명 |
|------|------|------|
| RV004 | 404 | 리뷰를 찾을 수 없음 |
| RV005 | 400 | 이미 답변 등록됨 |

### 4-5. 리뷰 삭제 (Admin)

```
DELETE /api/businesses/{businessId}/reviews/{reviewId}
인증: Bearer Token (Admin)
Content-Type: application/json
```

**Request Body (선택):**
```json
{
  "reason": "욕설/비방 포함"
}
```

**Response 200:**
```json
{
  "status": "success"
}
```

**Error Codes:**
| 코드 | HTTP | 설명 |
|------|------|------|
| RV004 | 404 | 리뷰를 찾을 수 없음 |
| RV006 | 400 | 이미 삭제된 리뷰 |

---

## 5. 알림 발송 이력 (Admin)

> 현재 카카오톡 실제 연동 전까지 **로그 기록만** 수행합니다.
> 향후 KakaoNotificationSender 구현 시 실제 발송으로 전환됩니다.

### 5-1. 알림 발송 이력 조회

```
GET /api/businesses/{businessId}/notification-logs
인증: Bearer Token (Admin)
```

**Query Parameters:**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| `channel` | String | X | - | 채널 필터: `KAKAO`, `SMS`, `EMAIL` |
| `status` | String | X | - | 상태 필터: `PENDING`, `SENT`, `FAILED` |
| `page` | Integer | X | 1 | 페이지 번호 |
| `size` | Integer | X | 20 | 페이지 크기 |

**Response 200:**
```json
{
  "status": "success",
  "data": {
    "content": [
      {
        "id": 1,
        "channel": "KAKAO",
        "templateType": "RESERVATION_CREATED",
        "recipientPhone": "010-9876-****",
        "recipientName": "박서연",
        "title": "예약 접수 알림",
        "status": "SENT",
        "sentAt": "2026-02-14T10:00:00",
        "reservationId": 201,
        "createdAt": "2026-02-14T10:00:00"
      }
    ],
    "pageInfo": {
      "page": 1,
      "size": 20,
      "totalElements": 350,
      "totalPages": 18
    }
  }
}
```

**알림 템플릿 타입:**
| 타입 | 발송 시점 | 설명 |
|------|----------|------|
| `RESERVATION_CREATED` | 예약 생성 시 | 예약 접수 알림 |
| `RESERVATION_CONFIRMED` | 관리자 확정 시 | 예약 확정 알림 |
| `RESERVATION_REMINDER` | 예약 D-1 | 예약 리마인더 |
| `RESERVATION_CHANGED` | 예약 변경 시 | 예약 변경 알림 |
| `RESERVATION_CANCELLED` | 예약 취소 시 | 예약 취소 알림 |
| `REVIEW_REQUEST` | 시술 완료 D+1 | 리뷰 작성 요청 |

---

## 6. DB 스키마 변경사항

### 6-1. Business 테이블 확장

```sql
-- 기존 businesses 테이블에 추가된 컬럼
ALTER TABLE businesses ADD COLUMN slug VARCHAR(100) UNIQUE;
ALTER TABLE businesses ADD COLUMN profile_image_url VARCHAR(500);
ALTER TABLE businesses ADD COLUMN gallery_images JSONB;
ALTER TABLE businesses ADD COLUMN latitude DOUBLE PRECISION;
ALTER TABLE businesses ADD COLUMN longitude DOUBLE PRECISION;
ALTER TABLE businesses ADD COLUMN tags VARCHAR(500);
ALTER TABLE businesses ADD COLUMN average_rating DOUBLE PRECISION DEFAULT 0;
ALTER TABLE businesses ADD COLUMN review_count INTEGER DEFAULT 0;
```

### 6-2. Reservation 테이블 확장

```sql
-- 예약 출처 구분
ALTER TABLE reservations ADD COLUMN source VARCHAR(20) DEFAULT 'ADMIN';
-- ADMIN: 관리자가 생성한 예약
-- ONLINE: 고객이 직접 생성한 예약
```

### 6-3. Reviews 테이블 (신규)

```sql
CREATE TABLE reviews (
    id BIGSERIAL PRIMARY KEY,
    business_id BIGINT NOT NULL,
    reservation_id BIGINT NOT NULL UNIQUE,
    customer_id BIGINT,
    staff_id BIGINT,
    customer_name VARCHAR(50) NOT NULL,
    customer_phone VARCHAR(20) NOT NULL,
    rating INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
    content TEXT,
    images JSONB DEFAULT '[]',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    reply_content TEXT,
    reply_created_at TIMESTAMP,
    delete_reason VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_reviews_business_id ON reviews(business_id);
CREATE INDEX idx_reviews_reservation_id ON reviews(reservation_id);
CREATE INDEX idx_reviews_customer_id ON reviews(customer_id);
CREATE INDEX idx_reviews_status ON reviews(status);
```

### 6-4. Notification Logs 테이블 (신규)

```sql
CREATE TABLE notification_logs (
    id BIGSERIAL PRIMARY KEY,
    business_id BIGINT NOT NULL,
    reservation_id BIGINT,
    channel VARCHAR(20) NOT NULL,
    template_type VARCHAR(50) NOT NULL,
    recipient_phone VARCHAR(20),
    recipient_name VARCHAR(50),
    title VARCHAR(200),
    content TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    error_message TEXT,
    sent_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notification_logs_business ON notification_logs(business_id);
CREATE INDEX idx_notification_logs_reservation ON notification_logs(reservation_id);
```

---

## 7. Error Code 전체 목록 (Phase 3)

### 고객 예약 (BK001 ~ BK010)
| 코드 | HTTP | 메시지 |
|------|------|--------|
| BK001 | 409 | 해당 시간대에 예약할 수 없습니다 |
| BK002 | 400 | 영업시간 외에는 예약할 수 없습니다 |
| BK003 | 400 | 휴무일에는 예약할 수 없습니다 |
| BK004 | 400 | 해당 스태프가 근무하지 않는 시간입니다 |
| BK005 | 400 | 취소 가능 시간이 지났습니다 |
| BK006 | 400 | 이미 취소된 예약입니다 |
| BK007 | 403 | 온라인 예약이 비활성화되어 있습니다 |
| BK008 | 400 | 예약 가능 기간을 초과했습니다 |
| BK009 | 400 | 최소 사전 예약 시간을 충족하지 않습니다 |
| BK010 | 403 | 전화번호가 일치하지 않습니다 |

### 슬러그 (BS001 ~ BS003)
| 코드 | HTTP | 메시지 |
|------|------|--------|
| BS001 | 409 | 이미 사용 중인 슬러그입니다 |
| BS002 | 400 | 슬러그 형식이 올바르지 않습니다 |
| BS003 | 400 | 사용할 수 없는 슬러그입니다 |

### 리뷰 (RV001 ~ RV006)
| 코드 | HTTP | 메시지 |
|------|------|--------|
| RV001 | 400 | 예약 정보가 일치하지 않습니다 |
| RV002 | 400 | 완료된 예약만 리뷰를 작성할 수 있습니다 |
| RV003 | 409 | 이미 리뷰가 작성된 예약입니다 |
| RV004 | 404 | 리뷰를 찾을 수 없습니다 |
| RV005 | 400 | 이미 답변이 등록된 리뷰입니다 |
| RV006 | 400 | 이미 삭제된 리뷰입니다 |

---

## 8. API 엔드포인트 요약

### Public API (인증 불필요)

| Method | URL | 설명 |
|--------|-----|------|
| `GET` | `/api/public/businesses` | 매장 검색/목록 |
| `GET` | `/api/public/businesses/check-slug` | 슬러그 사용 가능 확인 |
| `GET` | `/api/public/businesses/{slug}` | 매장 상세 조회 |
| `GET` | `/api/public/businesses/{slug}/available-dates` | 예약 가능 날짜 조회 |
| `GET` | `/api/public/businesses/{slug}/available-times` | 예약 가능 시간 조회 |
| `POST` | `/api/public/businesses/{slug}/reservations` | 고객 예약 생성 |
| `GET` | `/api/public/reservations/{reservationNumber}` | 예약 조회 |
| `POST` | `/api/public/reservations/{reservationNumber}/cancel` | 예약 취소 |
| `GET` | `/api/public/businesses/{slug}/reviews` | 리뷰 목록 조회 |
| `POST` | `/api/public/businesses/{slug}/reviews` | 리뷰 작성 |

### Admin API (인증 필요)

| Method | URL | 설명 |
|--------|-----|------|
| `PATCH` | `/api/businesses/{businessId}/slug` | 슬러그 변경 |
| `GET` | `/api/businesses/{businessId}/reviews` | 관리자 리뷰 목록 |
| `POST` | `/api/businesses/{businessId}/reviews/{reviewId}/reply` | 리뷰 답변 |
| `DELETE` | `/api/businesses/{businessId}/reviews/{reviewId}` | 리뷰 삭제 |
| `GET` | `/api/businesses/{businessId}/notification-logs` | 알림 발송 이력 |

---

## 9. 프론트엔드 구현 가이드

### 예약 플로우

```
1. 매장 검색/선택
   GET /api/public/businesses → 목록에서 선택
   GET /api/public/businesses/{slug} → 상세 정보 로딩

2. 서비스 선택
   매장 상세의 services 배열에서 선택

3. 날짜 선택 (캘린더)
   GET /api/public/businesses/{slug}/available-dates?month=2026-02&serviceId=1
   → hasSlots=true인 날짜만 선택 가능하게 표시

4. 시간/스태프 선택
   GET /api/public/businesses/{slug}/available-times?date=2026-02-20&serviceId=1
   → 가용 시간 슬롯 + 각 슬롯별 가용 스태프 목록

5. 고객 정보 입력 + 예약 생성
   POST /api/public/businesses/{slug}/reservations
   → 예약번호 반환

6. 예약 확인
   GET /api/public/reservations/{reservationNumber}?phone=010-9876-5432
```

### 리뷰 플로우

```
1. 리뷰 목록 조회 (매장 상세 하단)
   GET /api/public/businesses/{slug}/reviews

2. 리뷰 작성 (완료된 예약에서 접근)
   POST /api/public/businesses/{slug}/reviews
   → reservationNumber + phone으로 본인 확인
```

### 주의사항

1. **전화번호 형식**: `010-XXXX-XXXX` (하이픈 포함)
2. **날짜 형식**: `yyyy-MM-dd` (예: `2026-02-20`)
3. **시간 형식**: `HH:mm` (예: `14:00`)
4. **페이지 번호**: 1부터 시작 (0이 아님)
5. **슬러그 규칙**: 소문자, 숫자, 하이픈만 / 3~50자 / 시작·끝은 소문자나 숫자
6. **에러 응답 형식**:
```json
{
  "status": "error",
  "error": {
    "code": "BK001",
    "message": "해당 시간대에 예약할 수 없습니다"
  }
}
```

---

## 10. Swagger UI

모든 API는 Swagger UI에서 테스트 가능합니다:
```
http://localhost:8080/swagger-ui.html
```

**Tag 구분:**
- `Public Business` - 매장 검색/조회
- `Public Booking` - 예약 관련
- `Public Review` - 리뷰 (고객용)
- `Business Slug` - 슬러그 관리
- `Review Admin` - 리뷰 관리 (Admin)
- `Notification Log` - 알림 발송 이력
