# Phase 2: 백엔드 API 요청서 — 사용성 개선 + 기능 보강

> 서비스 운영 품질 향상
> 작성일: 2026-02-14
> 의존성: Phase 1 완료 후 진행

---

## 1. 스태프 근무 스케줄 API (신규)

> 현재 스태프는 기본 CRUD만 가능. 요일별 근무시간 설정 없음
> 캘린더에서 영업시간과 별도로 스태프별 가용 시간 필요

### 1-1. 스태프 근무 스케줄 조회

```
GET /api/businesses/{businessId}/staffs/{staffId}/schedules
Authorization: Bearer {token}

Response 200:
{
  "data": [
    {
      "id": 1,
      "dayOfWeek": 1,          // 1(월) ~ 7(일)
      "startTime": "10:00",
      "endTime": "19:00",
      "breakStartTime": "13:00",  // 선택
      "breakEndTime": "14:00",    // 선택
      "isWorking": true
    },
    {
      "id": 2,
      "dayOfWeek": 2,
      "startTime": "10:00",
      "endTime": "19:00",
      "breakStartTime": null,
      "breakEndTime": null,
      "isWorking": true
    },
    // ... 7일분
    {
      "id": 7,
      "dayOfWeek": 7,           // 일요일
      "startTime": null,
      "endTime": null,
      "breakStartTime": null,
      "breakEndTime": null,
      "isWorking": false         // 휴무
    }
  ]
}
```

### 1-2. 스태프 근무 스케줄 일괄 저장

```
PUT /api/businesses/{businessId}/staffs/{staffId}/schedules
Authorization: Bearer {token}

Request Body:
{
  "schedules": [
    { "dayOfWeek": 1, "startTime": "10:00", "endTime": "19:00", "breakStartTime": "13:00", "breakEndTime": "14:00", "isWorking": true },
    { "dayOfWeek": 2, "startTime": "10:00", "endTime": "19:00", "isWorking": true },
    { "dayOfWeek": 3, "startTime": "10:00", "endTime": "19:00", "isWorking": true },
    { "dayOfWeek": 4, "startTime": "10:00", "endTime": "19:00", "isWorking": true },
    { "dayOfWeek": 5, "startTime": "10:00", "endTime": "19:00", "isWorking": true },
    { "dayOfWeek": 6, "startTime": "10:00", "endTime": "17:00", "isWorking": true },
    { "dayOfWeek": 7, "isWorking": false }
  ]
}

Response 200:
{ "data": [ /* 저장된 스케줄 배열 */ ] }
```

### 1-3. 특정 날짜 스태프 가용 시간 조회

```
GET /api/businesses/{businessId}/staffs/{staffId}/available-times?date=2026-02-15
Authorization: Bearer {token}

Response 200:
{
  "data": {
    "date": "2026-02-15",
    "staffId": 1,
    "staffName": "김디자이너",
    "isWorking": true,
    "workStart": "10:00",
    "workEnd": "19:00",
    "breakStart": "13:00",
    "breakEnd": "14:00",
    "bookedSlots": [
      { "start": "10:00", "end": "11:00", "reservationId": 101 },
      { "start": "14:00", "end": "15:30", "reservationId": 105 }
    ],
    "availableSlots": [
      { "start": "11:00", "end": "13:00" },
      { "start": "15:30", "end": "19:00" }
    ]
  }
}
```

---

## 2. 스태프 이미지/포트폴리오 API (신규)

> 현재 스태프 프로필 사진은 URL 텍스트 입력만 가능 → 파일 업로드 필요
> 포트폴리오(시술 사진 갤러리) 기능 추가

### 2-1. 스태프 프로필 이미지 업로드

```
POST /api/businesses/{businessId}/staffs/{staffId}/profile-image
Authorization: Bearer {token}
Content-Type: multipart/form-data

Request:
  file: (이미지 파일, max 5MB, jpg/png/webp)

Response 200:
{
  "data": {
    "profileImageUrl": "/uploads/staffs/1_profile_20260214.jpg"
  }
}
```

### 2-2. 스태프 포트폴리오 목록 조회

```
GET /api/businesses/{businessId}/staffs/{staffId}/portfolio
Authorization: Bearer {token}

Query Params:
  page: 1
  size: 20

Response 200:
{
  "data": {
    "items": [
      {
        "id": 1,
        "imageUrl": "/uploads/staffs/1_portfolio_001.jpg",
        "thumbnailUrl": "/uploads/staffs/1_portfolio_001_thumb.jpg",
        "title": "레이어드컷",
        "description": "자연스러운 레이어드 컷 시술",
        "serviceCategory": "커트",
        "sortOrder": 1,
        "createdAt": "2026-02-10T14:00:00"
      }
    ],
    "totalCount": 15
  }
}
```

### 2-3. 스태프 포트폴리오 이미지 추가

```
POST /api/businesses/{businessId}/staffs/{staffId}/portfolio
Authorization: Bearer {token}
Content-Type: multipart/form-data

Request:
  file: (이미지 파일, max 10MB)
  title: "레이어드컷"           // 선택
  description: "시술 설명"      // 선택
  serviceCategory: "커트"       // 선택

Response 201:
{
  "data": {
    "id": 1,
    "imageUrl": "/uploads/staffs/1_portfolio_001.jpg",
    "thumbnailUrl": "/uploads/staffs/1_portfolio_001_thumb.jpg",
    "title": "레이어드컷",
    "createdAt": "2026-02-14T10:00:00"
  }
}
```

### 2-4. 스태프 포트폴리오 삭제

```
DELETE /api/businesses/{businessId}/staffs/{staffId}/portfolio/{portfolioId}
Authorization: Bearer {token}

Response 200:
{ "message": "포트폴리오 이미지가 삭제되었습니다." }
```

---

## 3. 고객 예약 이력 API (기존 확장)

> 현재 CustomerDetailDialog에 방문 통계만 표시
> 해당 고객의 과거 예약 목록이 필요

### 3-1. 특정 고객의 예약 이력 조회

```
GET /api/businesses/{businessId}/customers/{customerId}/reservations
Authorization: Bearer {token}

Query Params:
  page: 1
  size: 10
  status: COMPLETED    // 선택 (전체/COMPLETED/CANCELLED 등)
  sortBy: date         // date(최신순, 기본), amount(금액순)

Response 200:
{
  "data": {
    "items": [
      {
        "id": 101,
        "reservationDate": "2026-02-10",
        "startTime": "14:00",
        "endTime": "15:30",
        "staffName": "김디자이너",
        "services": ["여성컷", "트리트먼트"],
        "totalPrice": 50000,
        "status": "COMPLETED"
      }
    ],
    "totalCount": 24,
    "summary": {
      "totalVisits": 24,
      "totalSpent": 1200000,
      "lastVisitDate": "2026-02-10",
      "favoriteService": "여성컷",
      "favoriteStaff": "김디자이너"
    }
  }
}
```

> 기존 `GET /businesses/{businessId}/reservations` 에 `customerId` 파라미터 추가도 대안

---

## 4. 대시보드 기간별 통계 API (기존 확장)

> 현재 대시보드는 date 파라미터만 지원 (단일 날짜)
> 전주/전월 대비 통계, 기간 선택 기능 필요

### 4-1. 기간별 대시보드 통계

```
GET /api/businesses/{businessId}/dashboard/stats
Authorization: Bearer {token}

Query Params:
  startDate: 2026-02-01
  endDate: 2026-02-14
  compareWith: PREVIOUS_PERIOD  // 선택: PREVIOUS_PERIOD, PREVIOUS_YEAR

Response 200:
{
  "data": {
    "period": { "start": "2026-02-01", "end": "2026-02-14" },
    "stats": {
      "totalReservations": 156,
      "completedReservations": 140,
      "cancelledReservations": 8,
      "noShowReservations": 8,
      "totalRevenue": 12500000,
      "averageRevenuePerReservation": 89285,
      "newCustomers": 23,
      "returningCustomers": 117
    },
    "comparison": {
      "period": { "start": "2026-01-18", "end": "2026-01-31" },
      "reservationsChange": 12.5,    // % 증감
      "revenueChange": 8.3,
      "newCustomersChange": -5.2
    },
    "dailyBreakdown": [
      { "date": "2026-02-01", "reservations": 12, "revenue": 960000 },
      { "date": "2026-02-02", "reservations": 15, "revenue": 1200000 }
      // ...
    ]
  }
}
```

### 4-2. 목표 달성률

```
GET /api/businesses/{businessId}/dashboard/goals
Authorization: Bearer {token}

Query Params:
  month: 2026-02  // yyyy-MM

Response 200:
{
  "data": {
    "month": "2026-02",
    "revenueGoal": 20000000,
    "currentRevenue": 12500000,
    "revenueAchievementRate": 62.5,
    "reservationGoal": 300,
    "currentReservations": 156,
    "reservationAchievementRate": 52.0,
    "daysRemaining": 14,
    "projectedRevenue": 25000000,  // 현재 추세 기반 예측
    "projectedReservations": 312
  }
}
```

> 매장설정에서 목표(monthlyRevenueGoal, monthlyReservationGoal) 입력은 이미 가능하나, 달성률 계산 API가 없음

---

## 5. 캘린더 영업시간 연동 (기존 API 활용 — 확인 필요)

> 현재 캘린더에 영업시간 10:00-20:00 하드코딩
> `GET /businesses/{businessId}` 응답에 영업시간 포함 여부 확인 필요

### 확인 필요 사항

```
GET /api/businesses/{businessId} 응답에 포함되어야 할 영업시간 구조:

{
  "data": {
    "id": 1,
    "name": "미소 헤어",
    "businessType": "SALON",
    // ... 기본 정보 ...
    "businessHours": [
      { "dayOfWeek": 1, "openTime": "10:00", "closeTime": "20:00", "isOpen": true, "breakStart": "14:00", "breakEnd": "15:00" },
      { "dayOfWeek": 2, "openTime": "10:00", "closeTime": "20:00", "isOpen": true },
      // ... 7일분 ...
      { "dayOfWeek": 7, "isOpen": false }  // 일요일 휴무
    ]
  }
}
```

> 프론트엔드는 이 데이터를 캘린더 `businessHours`, `slotMinTime`, `slotMaxTime`에 반영 예정
> **질문**: 현재 `PATCH /businesses/{businessId}/settings` 로 영업시간을 저장하는데, 조회 시 같은 구조로 반환되나요?

---

## 6. 온보딩 상태 API (신규)

> 가입 직후 빈 대시보드만 보임 → 온보딩 위저드 필요
> 온보딩 완료 여부를 서버에서 트래킹

### 6-1. 온보딩 상태 조회

```
GET /api/businesses/{businessId}/onboarding
Authorization: Bearer {token}

Response 200:
{
  "data": {
    "isCompleted": false,
    "completedAt": null,
    "steps": [
      { "step": "BUSINESS_INFO", "label": "매장 정보 설정", "completed": true, "completedAt": "2026-02-14T10:00:00" },
      { "step": "SERVICES", "label": "서비스 등록", "completed": true, "completedAt": "2026-02-14T10:05:00" },
      { "step": "STAFFS", "label": "스태프 등록", "completed": false, "completedAt": null },
      { "step": "FIRST_RESERVATION", "label": "첫 예약 등록", "completed": false, "completedAt": null }
    ],
    "currentStep": "STAFFS"
  }
}
```

### 6-2. 온보딩 완료 처리 (스킵)

```
POST /api/businesses/{businessId}/onboarding/skip
Authorization: Bearer {token}

Response 200:
{ "message": "온보딩을 건너뛰었습니다." }
```

> 각 스텝 완료는 서비스/스태프/예약 생성 시 자동으로 업데이트되는 것이 이상적
> 프론트엔드에서 명시적으로 스텝 완료를 호출하지 않아도 됨

---

## 7. 우선순위 요약

| 순번 | 작업 | 규모 | 의존성 |
|------|------|------|--------|
| 1 | 캘린더 영업시간 확인 (5) | 소 | 없음 (확인만) |
| 2 | 고객 예약 이력 API (3) | 소 | 없음 |
| 3 | 대시보드 기간별 통계 (4-1) | 중 | 없음 |
| 4 | 대시보드 목표 달성률 (4-2) | 소 | 없음 |
| 5 | 스태프 근무 스케줄 (1) | 중 | 없음 |
| 6 | 스태프 이미지 업로드 (2-1) | 소 | 파일 스토리지 설정 |
| 7 | 스태프 포트폴리오 (2-2~2-4) | 중 | 파일 스토리지 설정 |
| 8 | 온보딩 상태 API (6) | 중 | 없음 |

### 파일 스토리지 참고
- Phase 1 프로필 이미지 + Phase 2 스태프 이미지 + 포트폴리오 모두 파일 업로드 필요
- S3/MinIO 또는 로컬 스토리지 중 선택 필요
- CDN 적용 시 응답에 CDN URL 반환
