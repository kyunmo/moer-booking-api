# Phase 3: 백엔드 API 요청서 — 고객용 예약 시스템

> 서비스 핵심 가치 실현 (B2C)
> 작성일: 2026-02-14
> 의존성: Phase 2 완료 후 진행 (스태프 스케줄, 포트폴리오 등)

---

## 개요

Phase 3는 **고객(소비자)**이 직접 예약할 수 있는 B2C 시스템 구축.
현재까지의 API는 모두 관리자(ADMIN) 인증 기반 — Phase 3에서는 **비인증/고객 인증** 기반 API 필요.

### 인증 구분

| 유형 | 설명 | 헤더 |
|------|------|------|
| Public | 누구나 접근 가능 (매장 검색, 정보 조회) | 없음 |
| Customer | 고객 로그인 필요 (예약, 리뷰 작성) | `Authorization: Bearer {customerToken}` |
| Admin | 관리자 로그인 필요 (기존 API) | `Authorization: Bearer {adminToken}` |

> 고객 계정은 관리자 계정과 분리? 또는 role로 구분? → **정책 결정 필요**

---

## 1. 매장 검색/목록 API (Public)

### 1-1. 매장 검색

```
GET /api/public/businesses
(인증 불필요)

Query Params:
  keyword: "미용실"           // 매장명, 주소 검색
  businessType: "SALON"      // SALON, PILATES, NAIL, MASSAGE, etc.
  lat: 37.5665               // 위도 (위치 기반 검색)
  lng: 126.9780              // 경도
  radius: 3                  // km 반경 (기본: 5km)
  sortBy: distance           // distance(거리순), rating(평점순), name(이름순)
  page: 1
  size: 20

Response 200:
{
  "data": {
    "items": [
      {
        "id": 1,
        "slug": "miso-hair",
        "name": "미소 헤어",
        "businessType": "SALON",
        "address": "서울시 강남구 역삼동 123-45",
        "phone": "02-1234-5678",
        "profileImageUrl": "/uploads/businesses/1_profile.jpg",
        "rating": 4.5,
        "reviewCount": 128,
        "distance": 1.2,          // km (위치 기반 검색 시)
        "isOpen": true,           // 현재 영업 중 여부
        "todayHours": "10:00 - 20:00",
        "priceRange": {
          "min": 15000,
          "max": 150000
        },
        "tags": ["예약가능", "주차가능", "카드결제"]
      }
    ],
    "totalCount": 45,
    "page": 1,
    "size": 20
  }
}
```

### 1-2. 매장 상세 정보 (Public)

```
GET /api/public/businesses/{slug}
(인증 불필요)

Response 200:
{
  "data": {
    "id": 1,
    "slug": "miso-hair",
    "name": "미소 헤어",
    "businessType": "SALON",
    "description": "강남 역삼동 프리미엄 헤어살롱",
    "address": "서울시 강남구 역삼동 123-45",
    "phone": "02-1234-5678",
    "profileImageUrl": "/uploads/businesses/1_profile.jpg",
    "galleryImages": [
      "/uploads/businesses/1_gallery_01.jpg",
      "/uploads/businesses/1_gallery_02.jpg"
    ],
    "rating": 4.5,
    "reviewCount": 128,

    "businessHours": [
      { "dayOfWeek": 1, "openTime": "10:00", "closeTime": "20:00", "isOpen": true },
      // ... 7일분
    ],

    "services": [
      {
        "id": 1,
        "categoryName": "커트",
        "name": "여성컷",
        "description": "샴푸 + 커트 + 드라이",
        "price": 30000,
        "duration": 60,        // 분
        "isPopular": true
      }
    ],

    "staffs": [
      {
        "id": 1,
        "name": "김디자이너",
        "title": "원장",
        "profileImageUrl": "/uploads/staffs/1_profile.jpg",
        "introduction": "15년 경력 헤어 디자이너",
        "rating": 4.8,
        "reviewCount": 45,
        "portfolioCount": 23,
        "specialties": ["여성컷", "펌", "염색"]
      }
    ],

    "reviews": {
      "averageRating": 4.5,
      "totalCount": 128,
      "ratingDistribution": {
        "5": 80, "4": 30, "3": 10, "2": 5, "1": 3
      },
      "recentReviews": [
        {
          "id": 1,
          "customerName": "김*소",       // 마스킹
          "rating": 5,
          "content": "정말 좋았어요!",
          "serviceName": "여성컷",
          "staffName": "김디자이너",
          "createdAt": "2026-02-10",
          "reply": {
            "content": "감사합니다!",
            "createdAt": "2026-02-11"
          }
        }
      ]
    }
  }
}
```

---

## 2. 예약 진행 API (Customer)

### 2-1. 예약 가능 날짜 조회

```
GET /api/public/businesses/{slug}/available-dates
(인증 불필요)

Query Params:
  staffId: 1          // 선택 (특정 스태프)
  serviceId: 1        // 선택 (특정 서비스)
  month: 2026-02      // yyyy-MM

Response 200:
{
  "data": {
    "month": "2026-02",
    "availableDates": [
      { "date": "2026-02-15", "hasSlots": true },
      { "date": "2026-02-16", "hasSlots": true },
      { "date": "2026-02-17", "hasSlots": false },  // 일요일 또는 만석
      // ...
    ]
  }
}
```

### 2-2. 예약 가능 시간 조회

```
GET /api/public/businesses/{slug}/available-times
(인증 불필요)

Query Params:
  date: 2026-02-15
  serviceId: 1         // 필수 (서비스 소요시간 기반으로 슬롯 계산)
  staffId: 1           // 선택 (미지정 시 가용 스태프 자동 배정)

Response 200:
{
  "data": {
    "date": "2026-02-15",
    "serviceDuration": 60,     // 분
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
      }
      // ...
    ]
  }
}
```

### 2-3. 예약 생성 (고객)

```
POST /api/public/businesses/{slug}/reservations
Content-Type: application/json

Request Body:
{
  "serviceIds": [1],
  "staffId": 1,                  // 선택 (미지정 시 자동 배정)
  "reservationDate": "2026-02-15",
  "startTime": "14:00",
  "customerName": "박서연",
  "customerPhone": "010-9876-5432",
  "customerEmail": "soyeon@email.com",  // 선택
  "customerRequest": "조용한 자리 부탁드려요",  // 선택
  "agreeToTerms": true
}

Response 201:
{
  "data": {
    "reservationId": 201,
    "reservationNumber": "R20260215-001",  // 고객에게 보여줄 번호
    "status": "PENDING",
    "confirmationMethod": "KAKAO",          // KAKAO, SMS, EMAIL
    "message": "예약이 접수되었습니다. 확정 시 알림을 보내드립니다."
  }
}

Error:
- BK001: 해당 시간대 예약 불가 (이미 예약됨)
- BK002: 영업시간 외 예약
- BK003: 휴무일 예약
- BK004: 해당 스태프 근무 불가
```

### 2-4. 예약 확인/조회 (고객)

```
GET /api/public/reservations/{reservationNumber}
(인증 불필요, 예약번호 + 전화번호로 조회)

Query Params:
  phone: 010-9876-5432

Response 200:
{
  "data": {
    "reservationNumber": "R20260215-001",
    "status": "CONFIRMED",
    "businessName": "미소 헤어",
    "businessAddress": "서울시 강남구 역삼동 123-45",
    "businessPhone": "02-1234-5678",
    "reservationDate": "2026-02-15",
    "startTime": "14:00",
    "endTime": "15:00",
    "staffName": "김디자이너",
    "services": ["여성컷"],
    "totalPrice": 30000,
    "canCancel": true,
    "cancelDeadline": "2026-02-14T20:00:00"  // 전날 20시까지 취소 가능
  }
}
```

### 2-5. 예약 취소 (고객)

```
POST /api/public/reservations/{reservationNumber}/cancel

Request Body:
{
  "phone": "010-9876-5432",
  "reason": "일정 변경"    // 선택
}

Response 200:
{
  "message": "예약이 취소되었습니다.",
  "data": {
    "reservationNumber": "R20260215-001",
    "status": "CANCELLED",
    "cancelledAt": "2026-02-14T10:00:00"
  }
}

Error:
- BK005: 취소 기한 초과
- BK006: 이미 취소/완료된 예약
```

---

## 3. 리뷰 시스템 API

### 3-1. 리뷰 작성 (고객)

```
POST /api/public/businesses/{slug}/reviews

Request Body:
{
  "reservationNumber": "R20260215-001",
  "phone": "010-9876-5432",          // 본인 확인
  "rating": 5,                        // 1~5
  "content": "정말 만족스러웠어요!",
  "staffId": 1,                       // 리뷰 대상 스태프
  "images": []                        // 선택, multipart 별도 또는 base64
}

Response 201:
{
  "data": {
    "reviewId": 1,
    "message": "리뷰가 등록되었습니다."
  }
}

Error:
- RV001: 예약 정보 불일치
- RV002: 완료되지 않은 예약 (COMPLETED만 리뷰 가능)
- RV003: 이미 리뷰 작성됨
```

### 3-2. 리뷰 목록 조회 (Public)

```
GET /api/public/businesses/{slug}/reviews
(인증 불필요)

Query Params:
  page: 1
  size: 10
  rating: 5          // 선택 (특정 별점 필터)
  staffId: 1         // 선택 (특정 스태프 리뷰)
  sortBy: latest     // latest, rating_high, rating_low
  hasPhoto: true     // 선택 (사진 리뷰만)

Response 200:
{
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
    "averageRating": 4.5,
    "ratingDistribution": { "5": 80, "4": 30, "3": 10, "2": 5, "1": 3 }
  }
}
```

### 3-3. 리뷰 답변 (Admin)

```
POST /api/businesses/{businessId}/reviews/{reviewId}/reply
Authorization: Bearer {adminToken}

Request Body:
{
  "content": "감사합니다! 또 방문해주세요 :)"
}

Response 200:
{
  "data": {
    "reviewId": 1,
    "reply": {
      "content": "감사합니다! 또 방문해주세요 :)",
      "createdAt": "2026-02-11T09:00:00"
    }
  }
}
```

### 3-4. 관리자 리뷰 목록 조회 (Admin)

```
GET /api/businesses/{businessId}/reviews
Authorization: Bearer {adminToken}

Query Params:
  page: 1
  size: 20
  status: ALL        // ALL, UNREPLIED, REPLIED
  rating: null       // 선택
  staffId: null      // 선택
  startDate: null    // 선택
  endDate: null      // 선택

Response 200:
{
  "data": {
    "items": [
      {
        "id": 1,
        "customerName": "박서연",       // 관리자에게는 전체 이름
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
        "createdAt": "2026-02-10T14:00:00"
      }
    ],
    "totalCount": 128,
    "stats": {
      "averageRating": 4.5,
      "totalReviews": 128,
      "unrepliedCount": 5,
      "thisMonthCount": 23
    }
  }
}
```

### 3-5. 리뷰 신고/삭제 (Admin)

```
DELETE /api/businesses/{businessId}/reviews/{reviewId}
Authorization: Bearer {adminToken}

Request Body:
{
  "reason": "욕설/비방 포함"
}

Response 200:
{ "message": "리뷰가 삭제 요청되었습니다." }
```

---

## 4. 카카오톡 알림 연동

> 카카오 알림톡 API 연동 필요

### 필요 템플릿

| 템플릿 | 발송 시점 | 내용 |
|--------|----------|------|
| 예약 접수 | 예약 생성 즉시 | 매장명, 날짜, 시간, 서비스, 담당자 |
| 예약 확정 | 관리자 확정 시 | 매장명, 날짜, 시간, 매장 전화번호 |
| 예약 리마인더 | 예약 D-1 | 내일 예약 안내, 매장 위치, 취소 링크 |
| 예약 변경 | 시간/스태프 변경 시 | 변경된 정보 |
| 예약 취소 | 취소 시 | 취소 확인, 재예약 링크 |
| 리뷰 요청 | 시술 완료 D+1 | 리뷰 작성 링크 |

### 백엔드 필요 작업

```
1. 카카오 비즈니스 채널 등록
   - 채널 ID 발급
   - 알림톡 템플릿 승인 (카카오 심사 3~5 영업일)

2. 카카오 알림톡 API 연동
   - API Key 발급
   - 발송 모듈 구현
   - 발송 실패 시 SMS fallback (선택)

3. 알림톡 발송 스케줄러
   - D-1 리마인더: 매일 오전 9시 배치
   - 리뷰 요청: 완료 D+1 배치
```

### 알림 발송 기록 API (Admin)

```
GET /api/businesses/{businessId}/notifications/history
Authorization: Bearer {adminToken}

Query Params:
  type: KAKAO          // KAKAO, SMS, EMAIL
  status: ALL          // ALL, SENT, FAILED
  page: 1
  size: 20

Response 200:
{
  "data": {
    "items": [
      {
        "id": 1,
        "type": "KAKAO",
        "template": "RESERVATION_CONFIRM",
        "recipientPhone": "010-9876-****",
        "status": "SENT",
        "sentAt": "2026-02-14T10:00:00",
        "reservationId": 201
      }
    ],
    "totalCount": 350
  }
}
```

---

## 5. 매장 슬러그(slug) 관리 (신규)

> 고객용 URL에 `businessId` 대신 사람이 읽을 수 있는 slug 사용
> 예: `/booking/miso-hair`

### 5-1. 슬러그 설정/수정 (Admin)

```
PATCH /api/businesses/{businessId}/slug
Authorization: Bearer {adminToken}

Request Body:
{
  "slug": "miso-hair"    // 영문 소문자, 숫자, 하이픈만 허용
}

Response 200:
{
  "data": {
    "slug": "miso-hair",
    "publicUrl": "https://yemo.io/booking/miso-hair"
  }
}

Error:
- BS001: 이미 사용 중인 슬러그
- BS002: 유효하지 않은 슬러그 형식
```

### 5-2. 슬러그 사용 가능 여부 확인

```
GET /api/public/businesses/check-slug?slug=miso-hair
(인증 불필요)

Response 200:
{
  "data": {
    "available": true,
    "suggestions": []
  }
}

// 사용 불가 시:
{
  "data": {
    "available": false,
    "suggestions": ["miso-hair-1", "miso-hair-gangnam"]
  }
}
```

---

## 6. 우선순위 요약

| 순번 | 작업 | 규모 | 의존성 |
|------|------|------|--------|
| 1 | 매장 슬러그 시스템 (5) | 소 | 없음 |
| 2 | 매장 검색/상세 Public API (1) | 중 | 슬러그 |
| 3 | 예약 가능 시간 조회 API (2-1, 2-2) | 중 | Phase 2 스태프 스케줄 |
| 4 | 고객 예약 생성/조회/취소 (2-3~2-5) | 중 | 가용시간 API |
| 5 | 리뷰 시스템 (3) | 중 | 예약 시스템 |
| 6 | 카카오 알림톡 연동 (4) | 대 | 카카오 비즈 채널 |
| 7 | 관리자 리뷰 관리 (3-3~3-5) | 소 | 리뷰 시스템 |

### 아키텍처 결정 사항

- [ ] 고객 계정 체계: 별도 테이블 vs role 구분 vs 비회원 예약만
- [ ] 예약 확정 방식: 자동 확정 vs 관리자 수동 확정 (매장별 설정)
- [ ] 슬러그 자동 생성: 매장 등록 시 자동 생성 vs 수동 입력
- [ ] 리뷰 노출 정책: 즉시 노출 vs 관리자 승인 후 노출
- [ ] 카카오톡 vs SMS: 카카오톡 실패 시 SMS fallback 여부
