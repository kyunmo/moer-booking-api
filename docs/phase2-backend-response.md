# Phase 2: 백엔드 API 구현 완료 응답서

> 작성일: 2026-02-14
> 요청서: `docs/phase2-backend-request.md`
> 상태: **모든 항목 구현 완료**

---

## 구현 요약

| # | 기능 | 상태 | 비고 |
|---|------|------|------|
| 1 | 스태프 근무 스케줄 API | **완료** | 요청서 1-1 ~ 1-3 모두 구현 |
| 2 | 스태프 이미지/포트폴리오 API | **완료** | 요청서 2-1 ~ 2-4 구현 (포트폴리오 경로 변경) |
| 3 | 고객 예약 이력 API | **완료** | 요청서 3-1 구현 |
| 4 | 대시보드 기간별 통계 + 목표 달성률 | **완료** | 요청서 4-1, 4-2 구현 |
| 5 | 캘린더 영업시간 연동 | **이미 구현됨** | 기존 API에서 businessHours 반환 중 |
| 6 | 온보딩 상태 API | **완료** | 요청서 6-1, 6-2 구현 + 자동 완료 |

---

## 1. 스태프 근무 스케줄 API

### 1-1. 근무 스케줄 조회

```
GET /api/businesses/{businessId}/staffs/{staffId}/schedules
Authorization: Bearer {token}
```

**Response 200:**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "staffId": 5,
      "dayOfWeek": 1,
      "dayName": "월",
      "startTime": "10:00",
      "endTime": "19:00",
      "breakStartTime": "13:00",
      "breakEndTime": "14:00",
      "isWorking": true
    },
    {
      "id": 2,
      "staffId": 5,
      "dayOfWeek": 2,
      "dayName": "화",
      "startTime": "10:00",
      "endTime": "19:00",
      "breakStartTime": null,
      "breakEndTime": null,
      "isWorking": true
    },
    {
      "id": 7,
      "staffId": 5,
      "dayOfWeek": 7,
      "dayName": "일",
      "startTime": null,
      "endTime": null,
      "breakStartTime": null,
      "breakEndTime": null,
      "isWorking": false
    }
  ]
}
```

**필드 설명:**

| 필드 | 타입 | 설명 |
|------|------|------|
| `dayOfWeek` | Integer | ISO-8601 요일 번호 (1=월 ~ 7=일) |
| `dayName` | String | 한글 요일명 ("월"~"일") |
| `startTime` | String (HH:mm) | 근무 시작 시간 |
| `endTime` | String (HH:mm) | 근무 종료 시간 |
| `breakStartTime` | String (HH:mm) | 휴게 시작 시간 (nullable) |
| `breakEndTime` | String (HH:mm) | 휴게 종료 시간 (nullable) |
| `isWorking` | Boolean | 해당 요일 근무 여부 |

> **참고**: 스케줄이 아직 등록되지 않은 스태프는 빈 배열 `[]` 반환

---

### 1-2. 근무 스케줄 일괄 저장

```
PUT /api/businesses/{businessId}/staffs/{staffId}/schedules
Authorization: Bearer {token}
Content-Type: application/json
```

**Request Body:**
```json
{
  "schedules": [
    { "dayOfWeek": 1, "startTime": "10:00", "endTime": "19:00", "breakStartTime": "13:00", "breakEndTime": "14:00", "isWorking": true },
    { "dayOfWeek": 2, "startTime": "10:00", "endTime": "19:00", "breakStartTime": null, "breakEndTime": null, "isWorking": true },
    { "dayOfWeek": 3, "startTime": "10:00", "endTime": "19:00", "breakStartTime": null, "breakEndTime": null, "isWorking": true },
    { "dayOfWeek": 4, "startTime": "10:00", "endTime": "19:00", "breakStartTime": null, "breakEndTime": null, "isWorking": true },
    { "dayOfWeek": 5, "startTime": "10:00", "endTime": "19:00", "breakStartTime": null, "breakEndTime": null, "isWorking": true },
    { "dayOfWeek": 6, "startTime": "10:00", "endTime": "17:00", "breakStartTime": null, "breakEndTime": null, "isWorking": true },
    { "dayOfWeek": 7, "startTime": null, "endTime": null, "breakStartTime": null, "breakEndTime": null, "isWorking": false }
  ]
}
```

**Validation:**
- `schedules`: 정확히 7개 항목 필수
- `dayOfWeek`: 1~7 필수
- `isWorking`: true/false 필수
- `isWorking=true`인 경우 `startTime`, `endTime` 필수
- `startTime` < `endTime` 필수
- `breakStartTime`, `breakEndTime`은 선택 (둘 다 있거나 둘 다 없어야 함)

**Response 200:** 저장된 스케줄 배열 (1-1과 동일 형식)

**동작 방식:** 기존 스케줄 전체 삭제 후 새로 삽입 (DELETE-INSERT)

---

### 1-3. 특정 날짜 가용 시간 조회

```
GET /api/businesses/{businessId}/staffs/{staffId}/available-times?date=2026-02-15
Authorization: Bearer {token}
```

**Query Params:**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `date` | LocalDate (yyyy-MM-dd) | O | 조회 날짜 |

**Response 200:**
```json
{
  "success": true,
  "data": {
    "staffId": 5,
    "staffName": "김디자이너",
    "date": "2026-02-15",
    "dayName": "토",
    "isWorkingDay": true,
    "workStart": "10:00",
    "workEnd": "17:00",
    "breakStart": null,
    "breakEnd": null,
    "bookedSlots": [
      { "start": "10:00", "end": "11:00", "reservationId": 101 },
      { "start": "14:00", "end": "15:30", "reservationId": 105 }
    ],
    "availableSlots": [
      { "startTime": "11:00", "endTime": "14:00" },
      { "startTime": "15:30", "endTime": "17:00" }
    ]
  }
}
```

**로직:**
1. 해당 날짜의 요일에 맞는 근무 스케줄 조회
2. 근무시간에서 휴게시간 제외
3. 기존 예약(PENDING/CONFIRMED)의 시간을 `bookedSlots`에 표시
4. 근무 - 휴게 - 예약 = `availableSlots` 계산

> **비근무일**: `isWorkingDay: false`, `bookedSlots: []`, `availableSlots: []`
> **스케줄 미등록 시**: `isWorkingDay: false` 반환

---

## 2. 스태프 이미지/포트폴리오 API

### 2-1. 프로필 이미지 업로드

```
POST /api/businesses/{businessId}/staffs/{staffId}/profile-image
Authorization: Bearer {token}
Content-Type: multipart/form-data
```

**Request:**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `file` | MultipartFile | O | 이미지 파일 (jpg/png/webp) |

**Response 200:**
```json
{
  "success": true,
  "data": {
    "id": 5,
    "businessId": 1,
    "name": "김디자이너",
    "position": "수석 디자이너",
    "phone": "010-1234-5678",
    "email": "kim@example.com",
    "specialty": "커트, 펌",
    "careerYears": 10,
    "profileImageUrl": "/uploads/staff-profiles/abc123.jpg",
    "introduction": "자연스러운 스타일을 추구합니다",
    "isActive": "Y",
    "createdAt": "2026-02-10T10:00:00"
  }
}
```

**동작:**
- 기존 프로필 이미지가 있으면 파일 시스템에서 삭제 후 새 이미지 저장
- 저장 경로: `C:/Project/uploads/staff-profiles/{uuid}.{ext}`
- URL 패턴: `/uploads/staff-profiles/{uuid}.{ext}`

---

### 2-2. 포트폴리오 목록 조회

기존 API를 그대로 사용합니다:

```
GET /api/businesses/{businessId}/staffs/{staffId}/portfolios
Authorization: Bearer {token}
```

**Response 200:**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "staffId": 5,
      "businessId": 1,
      "title": "레이어드컷",
      "description": "자연스러운 레이어드 컷 시술",
      "imageUrl": "/uploads/portfolios/abc123.jpg",
      "tags": ["커트", "레이어드"],
      "serviceCategory": "커트",
      "sortOrder": 1,
      "displayOrder": null,
      "isVisible": true,
      "createdAt": "2026-02-10 14:00:00"
    }
  ]
}
```

> **변경사항**: `serviceCategory`, `sortOrder` 필드가 추가되었습니다.

---

### 2-3. 포트폴리오 이미지 추가

```
POST /api/businesses/{businessId}/staffs/{staffId}/portfolios
Authorization: Bearer {token}
Content-Type: multipart/form-data
```

**Request:**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `file` | MultipartFile | O | 이미지 파일 |
| `title` | String | X | 제목 |
| `description` | String | X | 설명 |
| `serviceCategory` | String | X | 서비스 카테고리 (예: "커트", "펌", "컬러") |

**Response 200:**
```json
{
  "success": true,
  "data": {
    "id": 2,
    "staffId": 5,
    "businessId": 1,
    "title": "레이어드컷",
    "description": "자연스러운 레이어드 컷 시술",
    "imageUrl": "/uploads/portfolios/def456.jpg",
    "tags": [],
    "serviceCategory": "커트",
    "sortOrder": 0,
    "displayOrder": null,
    "isVisible": true,
    "createdAt": "2026-02-14 10:00:00"
  }
}
```

> **참고**: 요청서의 `thumbnailUrl` 필드는 현재 미구현입니다. 프론트엔드에서 이미지 리사이즈 필요 시 추후 추가 가능.

---

### 2-4. 포트폴리오 삭제

```
DELETE /api/businesses/{businessId}/staffs/{staffId}/portfolios/{portfolioId}
Authorization: Bearer {token}
```

**Response 200:**
```json
{
  "success": true,
  "message": "포트폴리오 이미지가 삭제되었습니다."
}
```

**동작:** DB 레코드 삭제 + 파일 시스템에서 이미지 파일 삭제

---

## 3. 고객 예약 이력 API

### 3-1. 고객별 예약 이력 조회

```
GET /api/businesses/{businessId}/customers/{customerId}/reservations
Authorization: Bearer {token}
```

**Query Params:**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| `page` | int | X | 1 | 페이지 번호 |
| `size` | int | X | 10 | 페이지 크기 |
| `status` | String | X | 전체 | 예약 상태 필터 (PENDING/CONFIRMED/COMPLETED/CANCELLED/NO_SHOW) |

**Response 200:**
```json
{
  "success": true,
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
      },
      {
        "id": 95,
        "reservationDate": "2026-01-25",
        "startTime": "11:00",
        "endTime": "12:00",
        "staffName": "박디자이너",
        "services": ["남성컷"],
        "totalPrice": 25000,
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

**필드 설명:**

| 필드 | 타입 | 설명 |
|------|------|------|
| `items[].services` | String[] | 예약에 포함된 서비스명 목록 (JSONB에서 추출) |
| `items[].staffName` | String | 담당 직원명 (nullable) |
| `summary.totalVisits` | Integer | 완료된 예약 총 횟수 |
| `summary.totalSpent` | Integer | 총 지출 금액 |
| `summary.favoriteService` | String | 가장 많이 이용한 서비스명 (JSONB 집계) |
| `summary.favoriteStaff` | String | 가장 많이 만난 직원명 |

> `favoriteService`는 예약 데이터의 JSONB `services` 배열에서 집계합니다.
> `favoriteStaff`는 해당 고객의 예약에서 가장 빈번한 `staff_id`를 기준으로 직원명을 조회합니다.

---

## 4. 대시보드 기간별 통계 + 목표 달성률

### 4-1. 기간별 통계

```
GET /api/businesses/{businessId}/dashboard/stats
Authorization: Bearer {token}
```

**Query Params:**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `startDate` | LocalDate (yyyy-MM-dd) | O | 시작일 |
| `endDate` | LocalDate (yyyy-MM-dd) | O | 종료일 |
| `compareWith` | String | X | 비교 기준: `PREVIOUS_PERIOD` 또는 `LAST_YEAR` |

**Response 200:**
```json
{
  "success": true,
  "data": {
    "period": {
      "start": "2026-02-01",
      "end": "2026-02-14"
    },
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
      "period": {
        "start": "2026-01-18",
        "end": "2026-01-31"
      },
      "reservationsChange": 12.5,
      "revenueChange": 8.3,
      "newCustomersChange": -5.2
    },
    "dailyBreakdown": [
      { "date": "2026-02-01", "reservations": 12, "revenue": 960000 },
      { "date": "2026-02-02", "reservations": 15, "revenue": 1200000 },
      { "date": "2026-02-03", "reservations": 8, "revenue": 640000 }
    ]
  }
}
```

**비교 로직:**

| compareWith | 비교 기간 계산 |
|-------------|---------------|
| `PREVIOUS_PERIOD` | 조회 기간과 동일한 길이의 직전 기간 (예: 2/1~2/14 → 1/18~1/31) |
| `LAST_YEAR` | 작년 동일 날짜 (예: 2026-02-01~14 → 2025-02-01~14) |
| 미지정 | `comparison` 필드 `null` |

**증감률 계산:** `((현재 - 이전) / 이전) * 100` (소수점 1자리, 이전 값 0이면 0.0)

**dailyBreakdown:**
- 조회 기간 내 매일의 예약 건수 + 매출 합계
- COMPLETED 상태인 예약의 `total_price` 합계 기준
- 예약이 없는 날짜도 포함 (reservations: 0, revenue: 0)

---

### 4-2. 목표 달성률

```
GET /api/businesses/{businessId}/dashboard/goals
Authorization: Bearer {token}
```

**Query Params:**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| `month` | String (yyyy-MM) | X | 현재 월 | 조회 월 |

**Response 200:**
```json
{
  "success": true,
  "data": {
    "month": "2026-02",
    "revenueGoal": 20000000,
    "currentRevenue": 12500000,
    "revenueAchievementRate": 62.5,
    "reservationGoal": 300,
    "currentReservations": 156,
    "reservationAchievementRate": 52.0,
    "daysRemaining": 14,
    "projectedRevenue": 25000000,
    "projectedReservations": 312
  }
}
```

**필드 설명:**

| 필드 | 타입 | 설명 |
|------|------|------|
| `revenueGoal` | Long | 매장설정의 `monthlyRevenueGoal` (미설정 시 0) |
| `currentRevenue` | Long | 해당 월 COMPLETED 예약 매출 합계 |
| `revenueAchievementRate` | Double | `(currentRevenue / revenueGoal) * 100` (목표 0이면 0.0) |
| `reservationGoal` | Integer | 매장설정의 `monthlyReservationGoal` (미설정 시 0) |
| `currentReservations` | Integer | 해당 월 전체 예약 건수 |
| `reservationAchievementRate` | Double | `(currentReservations / reservationGoal) * 100` |
| `daysRemaining` | Integer | 해당 월 잔여 일수 |
| `projectedRevenue` | Long | `(currentRevenue / 경과일) * 총일수` 추세 기반 예측 |
| `projectedReservations` | Integer | `(currentReservations / 경과일) * 총일수` 추세 기반 예측 |

> 매장설정(`business_settings`)에서 `monthly_revenue_goal`, `monthly_reservation_goal` 값을 사용합니다.
> 설정이 없거나 0인 경우 달성률은 0.0, 예측값은 추세 기반으로 계산됩니다.

---

## 5. 캘린더 영업시간 연동

### 현재 상태: **이미 구현되어 있음**

`GET /api/businesses/{businessId}` 응답에 `businessHours`가 JSONB로 포함됩니다:

```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "미소 헤어",
    "businessHours": {
      "monday": { "isOpen": true, "openTime": "10:00", "closeTime": "20:00", "breakStart": "14:00", "breakEnd": "15:00" },
      "tuesday": { "isOpen": true, "openTime": "10:00", "closeTime": "20:00" },
      "wednesday": { "isOpen": true, "openTime": "10:00", "closeTime": "20:00" },
      "thursday": { "isOpen": true, "openTime": "10:00", "closeTime": "20:00" },
      "friday": { "isOpen": true, "openTime": "10:00", "closeTime": "20:00" },
      "saturday": { "isOpen": true, "openTime": "10:00", "closeTime": "17:00" },
      "sunday": { "isOpen": false }
    }
  }
}
```

**프론트엔드 적용 방법:**
1. `GET /api/businesses/{businessId}`로 매장 정보 조회
2. `businessHours`에서 요일별 `openTime`/`closeTime` 추출
3. FullCalendar의 `businessHours`, `slotMinTime`, `slotMaxTime` 속성에 반영

> **주의**: `businessHours`는 JSONB 저장이므로 구조가 자유롭습니다. 위 구조는 현재 저장되는 기본 형태이며, `PATCH /api/businesses/{businessId}/settings`의 `businessHours` 필드로 저장합니다.

---

## 6. 온보딩 상태 API

### 6-1. 온보딩 상태 조회

```
GET /api/businesses/{businessId}/onboarding
Authorization: Bearer {token}
```

**Response 200:**
```json
{
  "success": true,
  "data": {
    "completed": false,
    "skipped": false,
    "steps": [
      { "step": "BUSINESS_INFO", "label": "매장 정보 설정", "completed": true },
      { "step": "SERVICES", "label": "서비스 등록", "completed": true },
      { "step": "STAFFS", "label": "스태프 등록", "completed": false },
      { "step": "FIRST_RESERVATION", "label": "첫 예약 등록", "completed": false }
    ],
    "currentStep": "STAFFS",
    "completedSteps": 2,
    "totalSteps": 4
  }
}
```

**필드 설명:**

| 필드 | 타입 | 설명 |
|------|------|------|
| `completed` | Boolean | 온보딩 전체 완료 여부 (모든 스텝 완료 시 자동 true) |
| `skipped` | Boolean | 온보딩 건너뛰기 여부 |
| `steps` | Array | 4개의 온보딩 단계 |
| `steps[].step` | String | 스텝 ID: `BUSINESS_INFO`, `SERVICES`, `STAFFS`, `FIRST_RESERVATION` |
| `steps[].label` | String | 사용자에게 표시할 라벨 |
| `steps[].completed` | Boolean | 해당 스텝 완료 여부 |
| `currentStep` | String | 현재 진행해야 할 스텝 (완료/스킵 시 `null`) |
| `completedSteps` | Integer | 완료된 스텝 수 |
| `totalSteps` | Integer | 전체 스텝 수 (4) |

**자동 완료 로직:**

| 스텝 | 자동 완료 조건 |
|------|----------------|
| `BUSINESS_INFO` | 매장 생성 시 자동 완료 (항상 true) |
| `SERVICES` | 첫 번째 서비스 등록 시 자동 완료 |
| `STAFFS` | 첫 번째 스태프 등록 시 자동 완료 |
| `FIRST_RESERVATION` | 첫 번째 예약 생성 시 자동 완료 |

> 프론트엔드에서 스텝 완료를 명시적으로 호출할 필요가 없습니다.
> 서비스/스태프/예약 생성 API 호출 시 백엔드에서 자동으로 해당 스텝을 완료 처리합니다.
> 4개 스텝 모두 완료되면 `completed`가 자동으로 `true`가 됩니다.

---

### 6-2. 온보딩 건너뛰기

```
POST /api/businesses/{businessId}/onboarding/skip
Authorization: Bearer {token}
```

**Response 200:**
```json
{
  "success": true,
  "message": "온보딩을 건너뛰었습니다."
}
```

**에러 케이스:**

| 상황 | ErrorCode | 메시지 |
|------|-----------|--------|
| 이미 완료된 온보딩 | OB001 | 온보딩이 이미 완료되었습니다 |
| 이미 건너뛴 온보딩 | OB002 | 온보딩이 이미 건너뛰기 처리되었습니다 |

---

## 프론트엔드 활용 가이드

### 온보딩 위저드 구현 흐름

```
1. 로그인 후 대시보드 진입
2. GET /api/businesses/{businessId}/onboarding 호출
3. completed === false && skipped === false 이면 온보딩 위저드 표시
4. currentStep에 따라 해당 설정 화면으로 안내
5. 사용자가 서비스/스태프/예약 등록하면 자동으로 다음 스텝 진행
6. 모든 스텝 완료 시 온보딩 자동 완료 → 대시보드 표시
7. "건너뛰기" 버튼 → POST .../onboarding/skip → 대시보드 표시
```

### 캘린더 영업시간 적용

```javascript
// 매장 정보 조회
const { data } = await api.get(`/businesses/${businessId}`)
const hours = data.businessHours

// FullCalendar 설정
const calendarOptions = {
  businessHours: Object.entries(hours)
    .filter(([_, v]) => v.isOpen)
    .map(([day, v]) => ({
      daysOfWeek: [dayToNumber(day)],  // 0=일, 1=월, ..., 6=토
      startTime: v.openTime,
      endTime: v.closeTime
    })),
  slotMinTime: getEarliestOpen(hours),   // 가장 빠른 오픈 시간
  slotMaxTime: getLatestClose(hours)     // 가장 늦은 마감 시간
}
```

### 고객 상세 다이얼로그 예약 이력

```javascript
// CustomerDetailDialog에서 예약 이력 탭 추가
const loadReservationHistory = async (customerId) => {
  const { data } = await api.get(
    `/businesses/${businessId}/customers/${customerId}/reservations`,
    { params: { page: 1, size: 10 } }
  )
  // data.items: 예약 목록
  // data.summary: 요약 통계 (favoriteService, favoriteStaff 등)
  // data.totalCount: 전체 건수 (페이지네이션용)
}
```

### 대시보드 기간 필터

```javascript
// 기간별 통계
const loadPeriodStats = async (startDate, endDate) => {
  const { data } = await api.get(
    `/businesses/${businessId}/dashboard/stats`,
    { params: { startDate, endDate, compareWith: 'PREVIOUS_PERIOD' } }
  )
  // data.stats: 현재 기간 통계
  // data.comparison: 이전 기간 대비 증감률
  // data.dailyBreakdown: 일별 차트 데이터
}

// 목표 달성률
const loadGoals = async () => {
  const { data } = await api.get(
    `/businesses/${businessId}/dashboard/goals`,
    { params: { month: '2026-02' } }
  )
  // data.revenueAchievementRate: 매출 달성률 (%)
  // data.projectedRevenue: 예상 매출
}
```

---

## DB 스키마 변경사항

### 신규 테이블

```sql
-- 스태프 근무 스케줄
CREATE TABLE staff_schedules (
    id BIGSERIAL PRIMARY KEY,
    staff_id BIGINT NOT NULL REFERENCES staffs(id),
    business_id BIGINT NOT NULL REFERENCES businesses(id),
    day_of_week INTEGER NOT NULL,           -- 1(월)~7(일)
    start_time TIME,
    end_time TIME,
    break_start_time TIME,
    break_end_time TIME,
    is_working CHAR(1) DEFAULT 'Y',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(staff_id, day_of_week)
);
```

### 기존 테이블 변경

```sql
-- portfolios 테이블에 컬럼 추가
ALTER TABLE portfolios ADD COLUMN service_category VARCHAR(100);
ALTER TABLE portfolios ADD COLUMN sort_order INTEGER DEFAULT 0;

-- business_settings 테이블에 온보딩 컬럼 추가
ALTER TABLE business_settings ADD COLUMN onboarding_completed CHAR(1) DEFAULT 'N';
ALTER TABLE business_settings ADD COLUMN onboarding_skipped CHAR(1) DEFAULT 'N';
ALTER TABLE business_settings ADD COLUMN onboarding_step_service CHAR(1) DEFAULT 'N';
ALTER TABLE business_settings ADD COLUMN onboarding_step_staff CHAR(1) DEFAULT 'N';
ALTER TABLE business_settings ADD COLUMN onboarding_step_reservation CHAR(1) DEFAULT 'N';
```

---

## 새로 추가된 ErrorCode

| 코드 | 이름 | 설명 |
|------|------|------|
| SS001 | SCHEDULE_NOT_FOUND | 근무 스케줄을 찾을 수 없습니다 |
| SS002 | INVALID_SCHEDULE_TIME | 근무 시간이 유효하지 않습니다 |
| SS003 | STAFF_NOT_WORKING_DAY | 해당 요일은 근무일이 아닙니다 |
| SS004 | NO_AVAILABLE_SLOTS | 예약 가능한 시간이 없습니다 |
| P002 | PORTFOLIO_IMAGE_REQUIRED | 포트폴리오 이미지는 필수입니다 |
| OB001 | ONBOARDING_ALREADY_COMPLETED | 온보딩이 이미 완료되었습니다 |
| OB002 | ONBOARDING_ALREADY_SKIPPED | 온보딩이 이미 건너뛰기 처리되었습니다 |

---

## 파일 업로드 설정

| 항목 | 값 |
|------|---|
| 저장 경로 | `C:/Project/uploads/` (로컬) |
| URL 패턴 | `/uploads/**` |
| 스태프 프로필 | `/uploads/staff-profiles/{filename}` |
| 포트폴리오 | `/uploads/portfolios/{filename}` |
| 최대 파일 크기 | `application.yml`의 `spring.servlet.multipart.max-file-size` 설정 |

---

## 요청서 대비 차이점

| 요청서 항목 | 구현 결과 | 비고 |
|------------|----------|------|
| 포트폴리오 경로 `/staffs/.../portfolio` | `/staffs/.../portfolios` (복수형) | 기존 포트폴리오 API와 일관성 유지 |
| 포트폴리오 `thumbnailUrl` | 미구현 | 추후 이미지 리사이즈 기능 추가 시 구현 가능 |
| 포트폴리오 페이징 | List 반환 | 포트폴리오 수가 적어 전체 조회로 충분 |
| 온보딩 `completedAt` 필드 | 미포함 | `completed: true/false`로 판단 가능 |
| 온보딩 `steps[].completedAt` | 미포함 | 완료 여부만 필요 |
| 온보딩 `isCompleted` | `completed` | 필드명 간소화 |
| 대시보드 `PREVIOUS_YEAR` | `LAST_YEAR` | 명칭 변경 |
