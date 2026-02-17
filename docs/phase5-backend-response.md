# Phase 5: 백엔드 API 응답서 -- 통계 분석 시스템

> 작성일: 2026-02-16
> 요청 문서: `docs/phase5-backend-request.md`
> 상태: **구현 완료**

---

## 개요

Phase 5 통계 분석 API 5종이 구현 완료되었습니다. 매출, 예약, 고객, 직원 성과, 서비스 분석에 대한 상세 데이터를 제공하며, 기간 비교(전기간/전년 동기) 기능을 포함합니다.

**구현 범위:**
- 매출 분석 API (매출 추이, 서비스별/결제수단별 매출, 목표 달성률)
- 예약 분석 API (예약 추이, 시간대별 히트맵, 상태 분포, 요일별 분포)
- 고객 분석 API (고객 증감 추이, 세그먼트, 재방문율, LTV 분포)
- 직원 성과 API (성과 비교, 매출 추이, 레이더 차트 데이터)
- 서비스 분석 API (서비스 랭킹, 카테고리 분포, 매출 추이)

---

## 아키텍처 결정 사항

| 항목 | 결정 | 근거 |
|------|------|------|
| 패키지 구조 | `domain.statistics` 단일 도메인 | 통계는 여러 도메인 데이터를 집계하므로 별도 도메인으로 분리 |
| 비교 기간 계산 | `PREVIOUS_PERIOD` / `LAST_YEAR` 서버 측 자동 계산 | 프론트에서 비교 기간을 직접 지정하지 않아도 됨 |
| Radar 차트 정규화 | 0~100 정수 스케일, 서버 측 정규화 | 프론트 차트 라이브러리에 즉시 바인딩 가능 |
| 히트맵 요일 기준 | ISO 8601 (1=월요일 ~ 7=일요일) | 국제 표준, Java `DayOfWeek` 호환 |
| 검색 조건 | `StatisticsSearchCondition` 공통 DTO | 5개 API 전체에서 재사용, 불필요한 파라미터는 각 API에서 무시 |
| 목표 달성률 | Business 엔티티의 `monthlyRevenueGoal` 활용 | 기존 대시보드 목표 설정과 일관성 유지 |
| 고객 세그먼트 기준 | VIP(10회+), 단골(3~9회), 신규(1회), 이탈(3개월 미방문) | 업계 표준 기준 적용 |
| 날짜 범위 제한 | 최대 365일 | 대량 데이터 집계 시 성능 보호 |

---

## 1. 매출 분석

```
GET /api/businesses/{businessId}/statistics/revenue
인증: Bearer Token (ADMIN, OWNER)
```

### Query Parameters

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| `startDate` | String | O | - | 시작일 (`yyyy-MM-dd`) |
| `endDate` | String | O | - | 종료일 (`yyyy-MM-dd`) |
| `groupBy` | String | X | `daily` | 집계 단위: `daily`, `weekly`, `monthly` |
| `compareWith` | String | X | - | 비교 기준: `PREVIOUS_PERIOD`, `LAST_YEAR` |

### Response 200

```json
{
  "status": "success",
  "data": {
    "summary": {
      "totalRevenue": 15620000,
      "averageRevenue": 557857,
      "averageTransactionAmount": 45000,
      "completionRate": 85.2,
      "customerLTV": 320000,
      "averageServiceDuration": 65
    },
    "comparison": {
      "revenueChange": 12.5,
      "averageRevenueChange": 8.3,
      "transactionAmountChange": 5.1,
      "completionRateChange": -2.0
    },
    "revenueTrend": [
      {
        "date": "2026-02-01",
        "revenue": 520000,
        "reservationCount": 12,
        "completedCount": 10
      },
      {
        "date": "2026-02-02",
        "revenue": 480000,
        "reservationCount": 11,
        "completedCount": 9
      },
      {
        "date": "2026-02-03",
        "revenue": 610000,
        "reservationCount": 14,
        "completedCount": 12
      }
    ],
    "revenueByService": [
      {
        "serviceId": 3,
        "serviceName": "펌",
        "categoryName": "헤어",
        "revenue": 5040000,
        "percentage": 32.3,
        "reservationCount": 42
      },
      {
        "serviceId": 1,
        "serviceName": "커트",
        "categoryName": "헤어",
        "revenue": 3500000,
        "percentage": 22.4,
        "reservationCount": 78
      },
      {
        "serviceId": 5,
        "serviceName": "염색",
        "categoryName": "헤어",
        "revenue": 2800000,
        "percentage": 17.9,
        "reservationCount": 28
      },
      {
        "serviceId": 8,
        "serviceName": "젤네일",
        "categoryName": "네일",
        "revenue": 1900000,
        "percentage": 12.2,
        "reservationCount": 38
      },
      {
        "serviceId": 10,
        "serviceName": "속눈썹연장",
        "categoryName": "속눈썹",
        "revenue": 1380000,
        "percentage": 8.8,
        "reservationCount": 23
      }
    ],
    "revenueByPaymentMethod": [
      {
        "method": "CARD",
        "methodName": "카드",
        "revenue": 12000000,
        "percentage": 76.8,
        "count": 267
      },
      {
        "method": "CASH",
        "methodName": "현금",
        "revenue": 2620000,
        "percentage": 16.8,
        "count": 58
      },
      {
        "method": "TRANSFER",
        "methodName": "계좌이체",
        "revenue": 1000000,
        "percentage": 6.4,
        "count": 22
      }
    ],
    "goals": {
      "revenueGoal": 20000000,
      "revenueAchievementRate": 78.1,
      "projectedRevenue": 18500000,
      "reservationGoal": null,
      "reservationAchievementRate": null,
      "projectedReservations": 380,
      "daysElapsed": 16,
      "daysRemaining": 12,
      "totalDays": 28
    }
  }
}
```

### 필드 설명

**summary (핵심 지표)**

| 필드 | 타입 | 설명 |
|------|------|------|
| `totalRevenue` | Long | 총 매출 (원) |
| `averageRevenue` | Long | 일평균 매출 (원) - 총매출 / 조회 기간 일수 |
| `averageTransactionAmount` | Long | 건당 평균 매출 (원) |
| `completionRate` | Double | 완료율 (%) - 소수점 1자리 |
| `customerLTV` | Long | 고객 평균 LTV (원) |
| `averageServiceDuration` | Integer | 평균 서비스 소요 시간 (분) |

**comparison (전기간 대비 변화율)** - `compareWith` 파라미터 전달 시에만 포함, 미전달 시 `null`

| 필드 | 타입 | 설명 |
|------|------|------|
| `revenueChange` | Double | 매출 변화율 (%) - 양수: 증가, 음수: 감소 |
| `averageRevenueChange` | Double | 일평균 매출 변화율 (%) |
| `transactionAmountChange` | Double | 건당 매출 변화율 (%) |
| `completionRateChange` | Double | 완료율 변화 (%) |

**revenueTrend (매출 추이)** - `groupBy` 단위로 집계

| 필드 | 타입 | 설명 |
|------|------|------|
| `date` | String | 날짜 (`2026-02-01` 또는 `2026-02` 형식) |
| `revenue` | Long | 해당 기간 매출 (원) |
| `reservationCount` | Integer | 전체 예약 수 |
| `completedCount` | Integer | 완료된 예약 수 |

**revenueByService (서비스별 매출)** - 매출 순 정렬, TOP 10

| 필드 | 타입 | 설명 |
|------|------|------|
| `serviceId` | Long | 서비스 ID |
| `serviceName` | String | 서비스명 |
| `categoryName` | String | 카테고리명 |
| `revenue` | Long | 해당 서비스 매출 (원) |
| `percentage` | Double | 전체 매출 대비 비중 (%) |
| `reservationCount` | Integer | 예약 수 |

**revenueByPaymentMethod (결제수단별 매출)**

| 필드 | 타입 | 설명 |
|------|------|------|
| `method` | String | 결제수단 코드 (`CARD`, `CASH`, `TRANSFER`) |
| `methodName` | String | 결제수단 한글명 |
| `revenue` | Long | 해당 결제수단 매출 (원) |
| `percentage` | Double | 전체 매출 대비 비중 (%) |
| `count` | Integer | 결제 건수 |

**goals (목표 달성률)** - `endDate`가 포함된 월 기준

| 필드 | 타입 | 설명 |
|------|------|------|
| `revenueGoal` | Long | 월 매출 목표 (원) - 미설정 시 `null` |
| `revenueAchievementRate` | Double | 매출 목표 달성률 (%) - 미설정 시 `null` |
| `projectedRevenue` | Long | 예상 월 매출 (원) - 현재 추세 기반 |
| `reservationGoal` | Integer | 월 예약 목표 - 미설정 시 `null` |
| `reservationAchievementRate` | Double | 예약 목표 달성률 (%) - 미설정 시 `null` |
| `projectedReservations` | Integer | 예상 월 예약 수 - 현재 추세 기반 |
| `daysElapsed` | Integer | 경과 일수 |
| `daysRemaining` | Integer | 남은 일수 |
| `totalDays` | Integer | 해당 월 총 일수 |

---

## 2. 예약 분석

```
GET /api/businesses/{businessId}/statistics/reservations
인증: Bearer Token (ADMIN, OWNER)
```

### Query Parameters

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| `startDate` | String | O | - | 시작일 (`yyyy-MM-dd`) |
| `endDate` | String | O | - | 종료일 (`yyyy-MM-dd`) |
| `groupBy` | String | X | `daily` | 집계 단위: `daily`, `weekly`, `monthly` |
| `compareWith` | String | X | - | 비교 기준: `PREVIOUS_PERIOD`, `LAST_YEAR` |

### Response 200

```json
{
  "status": "success",
  "data": {
    "summary": {
      "totalReservations": 347,
      "completedReservations": 295,
      "cancelledReservations": 32,
      "noShowReservations": 15,
      "pendingReservations": 5,
      "completionRate": 85.0,
      "cancellationRate": 9.2,
      "noShowRate": 4.3,
      "lostRevenue": 1420000
    },
    "comparison": {
      "totalChange": 8.5,
      "completedChange": 12.3,
      "cancelledChange": -15.2,
      "noShowChange": -5.0
    },
    "reservationTrend": [
      {
        "date": "2026-02-01",
        "total": 12,
        "completed": 10,
        "cancelled": 1,
        "noShow": 1,
        "pending": 0
      },
      {
        "date": "2026-02-02",
        "total": 11,
        "completed": 9,
        "cancelled": 2,
        "noShow": 0,
        "pending": 0
      },
      {
        "date": "2026-02-03",
        "total": 15,
        "completed": 13,
        "cancelled": 1,
        "noShow": 0,
        "pending": 1
      }
    ],
    "hourlyHeatmap": [
      {
        "dayOfWeek": 1,
        "dayName": "월",
        "hours": [
          { "hour": 9, "count": 3 },
          { "hour": 10, "count": 8 },
          { "hour": 11, "count": 12 },
          { "hour": 12, "count": 5 },
          { "hour": 13, "count": 7 },
          { "hour": 14, "count": 15 },
          { "hour": 15, "count": 18 },
          { "hour": 16, "count": 14 },
          { "hour": 17, "count": 10 },
          { "hour": 18, "count": 8 },
          { "hour": 19, "count": 5 },
          { "hour": 20, "count": 3 },
          { "hour": 21, "count": 1 }
        ]
      },
      {
        "dayOfWeek": 2,
        "dayName": "화",
        "hours": [
          { "hour": 9, "count": 4 },
          { "hour": 10, "count": 9 },
          { "hour": 11, "count": 11 },
          { "hour": 12, "count": 6 },
          { "hour": 13, "count": 8 },
          { "hour": 14, "count": 16 },
          { "hour": 15, "count": 17 },
          { "hour": 16, "count": 13 },
          { "hour": 17, "count": 11 },
          { "hour": 18, "count": 7 },
          { "hour": 19, "count": 4 },
          { "hour": 20, "count": 2 }
        ]
      },
      {
        "dayOfWeek": 3,
        "dayName": "수",
        "hours": [
          { "hour": 9, "count": 2 },
          { "hour": 10, "count": 7 },
          { "hour": 11, "count": 10 },
          { "hour": 12, "count": 4 },
          { "hour": 13, "count": 6 },
          { "hour": 14, "count": 13 },
          { "hour": 15, "count": 15 },
          { "hour": 16, "count": 12 },
          { "hour": 17, "count": 9 },
          { "hour": 18, "count": 6 },
          { "hour": 19, "count": 3 }
        ]
      },
      {
        "dayOfWeek": 4,
        "dayName": "목",
        "hours": [
          { "hour": 9, "count": 3 },
          { "hour": 10, "count": 8 },
          { "hour": 11, "count": 11 },
          { "hour": 12, "count": 5 },
          { "hour": 13, "count": 7 },
          { "hour": 14, "count": 14 },
          { "hour": 15, "count": 16 },
          { "hour": 16, "count": 13 },
          { "hour": 17, "count": 10 },
          { "hour": 18, "count": 7 },
          { "hour": 19, "count": 4 },
          { "hour": 20, "count": 2 }
        ]
      },
      {
        "dayOfWeek": 5,
        "dayName": "금",
        "hours": [
          { "hour": 9, "count": 5 },
          { "hour": 10, "count": 10 },
          { "hour": 11, "count": 14 },
          { "hour": 12, "count": 7 },
          { "hour": 13, "count": 9 },
          { "hour": 14, "count": 18 },
          { "hour": 15, "count": 20 },
          { "hour": 16, "count": 16 },
          { "hour": 17, "count": 12 },
          { "hour": 18, "count": 9 },
          { "hour": 19, "count": 6 },
          { "hour": 20, "count": 4 },
          { "hour": 21, "count": 2 }
        ]
      },
      {
        "dayOfWeek": 6,
        "dayName": "토",
        "hours": [
          { "hour": 10, "count": 12 },
          { "hour": 11, "count": 16 },
          { "hour": 12, "count": 8 },
          { "hour": 13, "count": 11 },
          { "hour": 14, "count": 20 },
          { "hour": 15, "count": 22 },
          { "hour": 16, "count": 18 },
          { "hour": 17, "count": 14 }
        ]
      },
      {
        "dayOfWeek": 7,
        "dayName": "일",
        "hours": [
          { "hour": 10, "count": 2 },
          { "hour": 11, "count": 3 },
          { "hour": 14, "count": 2 }
        ]
      }
    ],
    "statusDistribution": [
      { "status": "COMPLETED", "statusName": "완료", "count": 295, "percentage": 85.0 },
      { "status": "CANCELLED", "statusName": "취소", "count": 32, "percentage": 9.2 },
      { "status": "NO_SHOW", "statusName": "노쇼", "count": 15, "percentage": 4.3 },
      { "status": "PENDING", "statusName": "대기", "count": 5, "percentage": 1.5 }
    ],
    "dailyDistribution": [
      { "dayOfWeek": 1, "dayName": "월", "averageCount": 12.5, "totalCount": 50 },
      { "dayOfWeek": 2, "dayName": "화", "averageCount": 14.0, "totalCount": 56 },
      { "dayOfWeek": 3, "dayName": "수", "averageCount": 11.0, "totalCount": 44 },
      { "dayOfWeek": 4, "dayName": "목", "averageCount": 13.5, "totalCount": 54 },
      { "dayOfWeek": 5, "dayName": "금", "averageCount": 16.0, "totalCount": 64 },
      { "dayOfWeek": 6, "dayName": "토", "averageCount": 18.0, "totalCount": 72 },
      { "dayOfWeek": 7, "dayName": "일", "averageCount": 1.8, "totalCount": 7 }
    ]
  }
}
```

### 필드 설명

**summary (핵심 지표)**

| 필드 | 타입 | 설명 |
|------|------|------|
| `totalReservations` | Integer | 전체 예약 수 |
| `completedReservations` | Integer | 완료된 예약 수 |
| `cancelledReservations` | Integer | 취소된 예약 수 |
| `noShowReservations` | Integer | 노쇼 예약 수 |
| `pendingReservations` | Integer | 대기 중 예약 수 |
| `completionRate` | Double | 완료율 (%) |
| `cancellationRate` | Double | 취소율 (%) |
| `noShowRate` | Double | 노쇼율 (%) |
| `lostRevenue` | Long | 손실 매출 (원) - 취소+노쇼 기준 |

**comparison (전기간 대비 변화율)** - `compareWith` 파라미터 전달 시에만 포함, 미전달 시 `null`

| 필드 | 타입 | 설명 |
|------|------|------|
| `totalChange` | Double | 전체 예약 변화율 (%) |
| `completedChange` | Double | 완료 예약 변화율 (%) |
| `cancelledChange` | Double | 취소 예약 변화율 (%) |
| `noShowChange` | Double | 노쇼 예약 변화율 (%) |

**hourlyHeatmap (시간대별 히트맵)**

| 필드 | 타입 | 설명 |
|------|------|------|
| `dayOfWeek` | Integer | ISO 요일 (1=월 ~ 7=일) |
| `dayName` | String | 한글 요일명 |
| `hours` | Array | 시간별 예약 수 배열 |
| `hours[].hour` | Integer | 시간 (0~23) |
| `hours[].count` | Integer | 해당 시간대 예약 수 |

> **참고:** `hours` 배열에는 예약이 1건 이상 있는 시간만 포함됩니다. 예약이 0인 시간대는 배열에 포함되지 않으므로, 프론트에서 히트맵 렌더링 시 빈 시간대는 0으로 처리해야 합니다.

**statusDistribution (예약 상태 분포)** - Donut 차트용

| 필드 | 타입 | 설명 |
|------|------|------|
| `status` | String | 상태 코드 (`COMPLETED`, `CANCELLED`, `NO_SHOW`, `PENDING`) |
| `statusName` | String | 한글 상태명 |
| `count` | Integer | 해당 상태 예약 수 |
| `percentage` | Double | 비율 (%) |

**dailyDistribution (요일별 예약 분포)** - Bar 차트용

| 필드 | 타입 | 설명 |
|------|------|------|
| `dayOfWeek` | Integer | ISO 요일 (1=월 ~ 7=일) |
| `dayName` | String | 한글 요일명 |
| `averageCount` | Double | 해당 요일 평균 예약 수 |
| `totalCount` | Integer | 해당 요일 총 예약 수 |

---

## 3. 고객 분석

```
GET /api/businesses/{businessId}/statistics/customers
인증: Bearer Token (ADMIN, OWNER)
```

### Query Parameters

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| `startDate` | String | O | - | 시작일 (`yyyy-MM-dd`) |
| `endDate` | String | O | - | 종료일 (`yyyy-MM-dd`) |
| `compareWith` | String | X | - | 비교 기준: `PREVIOUS_PERIOD`, `LAST_YEAR` |

> **참고:** 고객 분석에서는 `groupBy` 파라미터를 사용하지 않습니다. 고객 추이(`customerTrend`)는 항상 월별로 집계됩니다.

### Response 200

```json
{
  "status": "success",
  "data": {
    "summary": {
      "totalCustomers": 245,
      "newCustomers": 32,
      "returningRate": 68.5,
      "averageVisitCount": 3.2,
      "averageLTV": 320000,
      "churnRate": 8.5
    },
    "comparison": {
      "totalCustomersChange": 5.2,
      "newCustomersChange": 15.0,
      "returningRateChange": 2.3,
      "averageVisitCountChange": -0.5
    },
    "customerTrend": [
      {
        "date": "2025-09",
        "newCustomers": 25,
        "returningCustomers": 142,
        "totalActive": 167,
        "churned": 10
      },
      {
        "date": "2025-10",
        "newCustomers": 28,
        "returningCustomers": 148,
        "totalActive": 176,
        "churned": 8
      },
      {
        "date": "2025-11",
        "newCustomers": 22,
        "returningCustomers": 152,
        "totalActive": 174,
        "churned": 11
      },
      {
        "date": "2025-12",
        "newCustomers": 30,
        "returningCustomers": 155,
        "totalActive": 185,
        "churned": 9
      },
      {
        "date": "2026-01",
        "newCustomers": 28,
        "returningCustomers": 156,
        "totalActive": 184,
        "churned": 12
      },
      {
        "date": "2026-02",
        "newCustomers": 32,
        "returningCustomers": 162,
        "totalActive": 194,
        "churned": 7
      }
    ],
    "segments": [
      {
        "segment": "VIP",
        "segmentName": "VIP",
        "description": "10회 이상 방문",
        "count": 15,
        "percentage": 6.1,
        "totalRevenue": 4800000,
        "averageRevenue": 320000
      },
      {
        "segment": "REGULAR",
        "segmentName": "단골",
        "description": "3~9회 방문",
        "count": 68,
        "percentage": 27.8,
        "totalRevenue": 8160000,
        "averageRevenue": 120000
      },
      {
        "segment": "NEW",
        "segmentName": "신규",
        "description": "1회 방문",
        "count": 120,
        "percentage": 49.0,
        "totalRevenue": 5400000,
        "averageRevenue": 45000
      },
      {
        "segment": "INACTIVE",
        "segmentName": "이탈",
        "description": "3개월 미방문",
        "count": 42,
        "percentage": 17.1,
        "totalRevenue": 0,
        "averageRevenue": 0
      }
    ],
    "returningRateTrend": [
      { "date": "2025-09", "rate": 62.0 },
      { "date": "2025-10", "rate": 64.5 },
      { "date": "2025-11", "rate": 65.0 },
      { "date": "2025-12", "rate": 66.8 },
      { "date": "2026-01", "rate": 67.2 },
      { "date": "2026-02", "rate": 68.5 }
    ],
    "ltvDistribution": [
      { "range": "0~5만", "min": 0, "max": 50000, "count": 85 },
      { "range": "5~10만", "min": 50000, "max": 100000, "count": 62 },
      { "range": "10~30만", "min": 100000, "max": 300000, "count": 55 },
      { "range": "30~50만", "min": 300000, "max": 500000, "count": 28 },
      { "range": "50만 이상", "min": 500000, "max": null, "count": 15 }
    ]
  }
}
```

### 필드 설명

**summary (핵심 지표)**

| 필드 | 타입 | 설명 |
|------|------|------|
| `totalCustomers` | Integer | 기간 내 방문 고객 수 (중복 제거) |
| `newCustomers` | Integer | 기간 내 신규 고객 수 |
| `returningRate` | Double | 재방문율 (%) |
| `averageVisitCount` | Double | 고객 평균 방문 횟수 |
| `averageLTV` | Long | 고객 평균 LTV (원) |
| `churnRate` | Double | 이탈률 (%) - 3개월 미방문 기준 |

**comparison (전기간 대비 변화율)** - `compareWith` 파라미터 전달 시에만 포함, 미전달 시 `null`

| 필드 | 타입 | 설명 |
|------|------|------|
| `totalCustomersChange` | Double | 전체 고객 변화율 (%) |
| `newCustomersChange` | Double | 신규 고객 변화율 (%) |
| `returningRateChange` | Double | 재방문율 변화 (%) |
| `averageVisitCountChange` | Double | 평균 방문 횟수 변화율 (%) |

**customerTrend (고객 증감 추이)** - 항상 월별 집계

| 필드 | 타입 | 설명 |
|------|------|------|
| `date` | String | 년월 (`2026-02` 형식) |
| `newCustomers` | Integer | 해당 월 신규 고객 수 |
| `returningCustomers` | Integer | 해당 월 재방문 고객 수 |
| `totalActive` | Integer | 해당 월 활성 고객 수 |
| `churned` | Integer | 해당 월 이탈 고객 수 |

**segments (고객 세그먼트)**

| 필드 | 타입 | 설명 |
|------|------|------|
| `segment` | String | 세그먼트 코드 (`VIP`, `REGULAR`, `NEW`, `INACTIVE`) |
| `segmentName` | String | 한글 세그먼트명 |
| `description` | String | 세그먼트 설명 |
| `count` | Integer | 해당 세그먼트 고객 수 |
| `percentage` | Double | 전체 대비 비율 (%) |
| `totalRevenue` | Long | 해당 세그먼트 총 매출 (원) |
| `averageRevenue` | Long | 해당 세그먼트 고객 평균 매출 (원) |

**세그먼트 분류 기준:**

| 세그먼트 | 기준 |
|----------|------|
| VIP | 10회 이상 방문 |
| REGULAR (단골) | 3~9회 방문 |
| NEW (신규) | 1회 방문 |
| INACTIVE (이탈) | 3개월 이상 미방문 |

**returningRateTrend (재방문율 추이)** - 월별

| 필드 | 타입 | 설명 |
|------|------|------|
| `date` | String | 년월 (`2026-02` 형식) |
| `rate` | Double | 해당 월 재방문율 (%) |

**ltvDistribution (고객 LTV 분포)** - Histogram 차트용

| 필드 | 타입 | 설명 |
|------|------|------|
| `range` | String | 구간 표시 (한글) |
| `min` | Long | 구간 최솟값 (원) |
| `max` | Long | 구간 최댓값 (원) - 마지막 구간은 `null` |
| `count` | Integer | 해당 구간 고객 수 |

---

## 4. 직원 성과

```
GET /api/businesses/{businessId}/statistics/staff
인증: Bearer Token (ADMIN, OWNER)
```

### Query Parameters

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| `startDate` | String | O | - | 시작일 (`yyyy-MM-dd`) |
| `endDate` | String | O | - | 종료일 (`yyyy-MM-dd`) |
| `staffId` | Long | X | - | 특정 직원 필터 (전달 시 해당 직원만 조회) |
| `compareWith` | String | X | - | 비교 기준: `PREVIOUS_PERIOD`, `LAST_YEAR` |

### Response 200

```json
{
  "status": "success",
  "data": {
    "staffPerformances": [
      {
        "staffId": 1,
        "staffName": "김디자이너",
        "positionName": "원장",
        "profileImageUrl": "/uploads/staff/1/profile.jpg",
        "reservationCount": 45,
        "completedCount": 42,
        "cancelledCount": 2,
        "noShowCount": 1,
        "totalRevenue": 3500000,
        "averageRevenue": 83333,
        "averageDuration": 65,
        "completionRate": 93.3,
        "customerCount": 38
      },
      {
        "staffId": 2,
        "staffName": "이실장",
        "positionName": "실장",
        "profileImageUrl": null,
        "reservationCount": 38,
        "completedCount": 35,
        "cancelledCount": 2,
        "noShowCount": 1,
        "totalRevenue": 2850000,
        "averageRevenue": 81428,
        "averageDuration": 58,
        "completionRate": 92.1,
        "customerCount": 32
      },
      {
        "staffId": 3,
        "staffName": "박주니어",
        "positionName": "디자이너",
        "profileImageUrl": null,
        "reservationCount": 28,
        "completedCount": 25,
        "cancelledCount": 2,
        "noShowCount": 1,
        "totalRevenue": 1680000,
        "averageRevenue": 67200,
        "averageDuration": 52,
        "completionRate": 89.3,
        "customerCount": 24
      }
    ],
    "comparison": {
      "totalReservationsChange": 8.5,
      "totalRevenueChange": 12.0
    },
    "staffRevenueTrend": [
      {
        "staffId": 1,
        "staffName": "김디자이너",
        "trend": [
          { "date": "2025-09", "revenue": 2800000, "reservationCount": 35 },
          { "date": "2025-10", "revenue": 3100000, "reservationCount": 39 },
          { "date": "2025-11", "revenue": 3200000, "reservationCount": 41 },
          { "date": "2025-12", "revenue": 3400000, "reservationCount": 43 },
          { "date": "2026-01", "revenue": 3300000, "reservationCount": 42 },
          { "date": "2026-02", "revenue": 3500000, "reservationCount": 45 }
        ]
      },
      {
        "staffId": 2,
        "staffName": "이실장",
        "trend": [
          { "date": "2025-09", "revenue": 2200000, "reservationCount": 30 },
          { "date": "2025-10", "revenue": 2400000, "reservationCount": 32 },
          { "date": "2025-11", "revenue": 2500000, "reservationCount": 33 },
          { "date": "2025-12", "revenue": 2700000, "reservationCount": 35 },
          { "date": "2026-01", "revenue": 2600000, "reservationCount": 34 },
          { "date": "2026-02", "revenue": 2850000, "reservationCount": 38 }
        ]
      },
      {
        "staffId": 3,
        "staffName": "박주니어",
        "trend": [
          { "date": "2025-09", "revenue": 1200000, "reservationCount": 20 },
          { "date": "2025-10", "revenue": 1350000, "reservationCount": 22 },
          { "date": "2025-11", "revenue": 1400000, "reservationCount": 23 },
          { "date": "2025-12", "revenue": 1500000, "reservationCount": 25 },
          { "date": "2026-01", "revenue": 1550000, "reservationCount": 26 },
          { "date": "2026-02", "revenue": 1680000, "reservationCount": 28 }
        ]
      }
    ],
    "staffRadar": [
      {
        "staffId": 1,
        "staffName": "김디자이너",
        "metrics": {
          "reservationVolume": 100,
          "revenue": 100,
          "completionRate": 93,
          "customerSatisfaction": 90,
          "efficiency": 80
        }
      },
      {
        "staffId": 2,
        "staffName": "이실장",
        "metrics": {
          "reservationVolume": 84,
          "revenue": 81,
          "completionRate": 92,
          "customerSatisfaction": 86,
          "efficiency": 90
        }
      },
      {
        "staffId": 3,
        "staffName": "박주니어",
        "metrics": {
          "reservationVolume": 62,
          "revenue": 48,
          "completionRate": 89,
          "customerSatisfaction": 78,
          "efficiency": 100
        }
      }
    ]
  }
}
```

### 필드 설명

**staffPerformances (직원별 성과)** - 매출 순 정렬

| 필드 | 타입 | 설명 |
|------|------|------|
| `staffId` | Long | 직원 ID |
| `staffName` | String | 직원명 |
| `positionName` | String | 직급명 |
| `profileImageUrl` | String | 프로필 이미지 URL (없으면 `null`) |
| `reservationCount` | Integer | 전체 예약 수 |
| `completedCount` | Integer | 완료 수 |
| `cancelledCount` | Integer | 취소 수 |
| `noShowCount` | Integer | 노쇼 수 |
| `totalRevenue` | Long | 총 매출 (원) |
| `averageRevenue` | Long | 건당 평균 매출 (원) |
| `averageDuration` | Integer | 평균 서비스 시간 (분) |
| `completionRate` | Double | 완료율 (%) |
| `customerCount` | Integer | 담당 고객 수 (중복 제거) |

**comparison (전기간 대비 변화율)** - `compareWith` 파라미터 전달 시에만 포함, 미전달 시 `null`

| 필드 | 타입 | 설명 |
|------|------|------|
| `totalReservationsChange` | Double | 전체 예약 수 변화율 (%) |
| `totalRevenueChange` | Double | 전체 매출 변화율 (%) |

**staffRevenueTrend (직원별 매출 추이)** - 월별 집계

| 필드 | 타입 | 설명 |
|------|------|------|
| `staffId` | Long | 직원 ID |
| `staffName` | String | 직원명 |
| `trend` | Array | 월별 추이 데이터 |
| `trend[].date` | String | 년월 (`2026-02` 형식) |
| `trend[].revenue` | Long | 해당 월 매출 (원) |
| `trend[].reservationCount` | Integer | 해당 월 예약 수 |

**staffRadar (직원별 역량 레이더)** - Radar 차트용, 0~100 정수 스케일

| 필드 | 타입 | 설명 |
|------|------|------|
| `staffId` | Long | 직원 ID |
| `staffName` | String | 직원명 |
| `metrics.reservationVolume` | Integer | 예약량 (0~100) - 최다 예약자 대비 비율 |
| `metrics.revenue` | Integer | 매출 (0~100) - 최고 매출자 대비 비율 |
| `metrics.completionRate` | Integer | 완료율 (0~100) - 실제 완료율 반올림 |
| `metrics.customerSatisfaction` | Integer | 고객 만족도 (0~100) - 평균 평점/5.0*100 |
| `metrics.efficiency` | Integer | 효율성 (0~100) - 짧은 소요시간일수록 높음 |

> **Radar 차트 정규화 방식:**
> - `reservationVolume`: (해당 직원 예약 수 / 최다 예약 직원 수) * 100
> - `revenue`: (해당 직원 매출 / 최고 매출 직원) * 100
> - `completionRate`: 실제 완료율 반올림 (예: 93.3% -> 93)
> - `customerSatisfaction`: (평균 평점 / 5.0) * 100
> - `efficiency`: (전체 직원 중 최소 소요시간 / 해당 직원 소요시간) * 100

---

## 5. 서비스 분석

```
GET /api/businesses/{businessId}/statistics/services
인증: Bearer Token (ADMIN, OWNER)
```

### Query Parameters

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| `startDate` | String | O | - | 시작일 (`yyyy-MM-dd`) |
| `endDate` | String | O | - | 종료일 (`yyyy-MM-dd`) |
| `categoryId` | Long | X | - | 카테고리 필터 (전달 시 해당 카테고리 서비스만 조회) |
| `compareWith` | String | X | - | 비교 기준: `PREVIOUS_PERIOD`, `LAST_YEAR` |

### Response 200

```json
{
  "status": "success",
  "data": {
    "summary": {
      "totalServiceCount": 347,
      "uniqueServiceTypes": 12,
      "averagePrice": 45000,
      "categoryCount": 3,
      "mostPopularService": "커트",
      "mostProfitableService": "펌"
    },
    "comparison": {
      "totalServiceCountChange": 8.5,
      "averagePriceChange": 2.1
    },
    "serviceRankings": [
      {
        "rank": 1,
        "serviceId": 3,
        "serviceName": "펌",
        "categoryId": 1,
        "categoryName": "헤어",
        "reservationCount": 42,
        "totalRevenue": 5040000,
        "averagePrice": 120000,
        "revenuePercentage": 32.3,
        "averageDuration": 120,
        "completionRate": 88.1
      },
      {
        "rank": 2,
        "serviceId": 1,
        "serviceName": "커트",
        "categoryId": 1,
        "categoryName": "헤어",
        "reservationCount": 78,
        "totalRevenue": 3500000,
        "averagePrice": 44871,
        "revenuePercentage": 22.4,
        "averageDuration": 35,
        "completionRate": 92.3
      },
      {
        "rank": 3,
        "serviceId": 5,
        "serviceName": "염색",
        "categoryId": 1,
        "categoryName": "헤어",
        "reservationCount": 28,
        "totalRevenue": 2800000,
        "averagePrice": 100000,
        "revenuePercentage": 17.9,
        "averageDuration": 90,
        "completionRate": 85.7
      },
      {
        "rank": 4,
        "serviceId": 8,
        "serviceName": "젤네일",
        "categoryId": 2,
        "categoryName": "네일",
        "reservationCount": 38,
        "totalRevenue": 1900000,
        "averagePrice": 50000,
        "revenuePercentage": 12.2,
        "averageDuration": 60,
        "completionRate": 94.7
      },
      {
        "rank": 5,
        "serviceId": 10,
        "serviceName": "속눈썹연장",
        "categoryId": 3,
        "categoryName": "속눈썹",
        "reservationCount": 23,
        "totalRevenue": 1380000,
        "averagePrice": 60000,
        "revenuePercentage": 8.8,
        "averageDuration": 75,
        "completionRate": 91.3
      }
    ],
    "categoryDistribution": [
      {
        "categoryId": 1,
        "categoryName": "헤어",
        "serviceCount": 5,
        "reservationCount": 180,
        "revenue": 10500000,
        "percentage": 67.2
      },
      {
        "categoryId": 2,
        "categoryName": "네일",
        "serviceCount": 4,
        "reservationCount": 95,
        "revenue": 3325000,
        "percentage": 21.3
      },
      {
        "categoryId": 3,
        "categoryName": "속눈썹",
        "serviceCount": 3,
        "reservationCount": 72,
        "revenue": 1800000,
        "percentage": 11.5
      }
    ],
    "serviceTrend": [
      {
        "serviceId": 3,
        "serviceName": "펌",
        "trend": [
          { "date": "2025-09", "revenue": 3600000, "count": 30 },
          { "date": "2025-10", "revenue": 4080000, "count": 34 },
          { "date": "2025-11", "revenue": 4200000, "count": 35 },
          { "date": "2025-12", "revenue": 4560000, "count": 38 },
          { "date": "2026-01", "revenue": 4800000, "count": 40 },
          { "date": "2026-02", "revenue": 5040000, "count": 42 }
        ]
      },
      {
        "serviceId": 1,
        "serviceName": "커트",
        "trend": [
          { "date": "2025-09", "revenue": 2800000, "count": 62 },
          { "date": "2025-10", "revenue": 3100000, "count": 69 },
          { "date": "2025-11", "revenue": 3000000, "count": 67 },
          { "date": "2025-12", "revenue": 3200000, "count": 71 },
          { "date": "2026-01", "revenue": 3400000, "count": 76 },
          { "date": "2026-02", "revenue": 3500000, "count": 78 }
        ]
      },
      {
        "serviceId": 5,
        "serviceName": "염색",
        "trend": [
          { "date": "2025-09", "revenue": 2000000, "count": 20 },
          { "date": "2025-10", "revenue": 2200000, "count": 22 },
          { "date": "2025-11", "revenue": 2300000, "count": 23 },
          { "date": "2025-12", "revenue": 2500000, "count": 25 },
          { "date": "2026-01", "revenue": 2600000, "count": 26 },
          { "date": "2026-02", "revenue": 2800000, "count": 28 }
        ]
      },
      {
        "serviceId": 8,
        "serviceName": "젤네일",
        "trend": [
          { "date": "2025-09", "revenue": 1350000, "count": 27 },
          { "date": "2025-10", "revenue": 1500000, "count": 30 },
          { "date": "2025-11", "revenue": 1550000, "count": 31 },
          { "date": "2025-12", "revenue": 1650000, "count": 33 },
          { "date": "2026-01", "revenue": 1750000, "count": 35 },
          { "date": "2026-02", "revenue": 1900000, "count": 38 }
        ]
      },
      {
        "serviceId": 10,
        "serviceName": "속눈썹연장",
        "trend": [
          { "date": "2025-09", "revenue": 960000, "count": 16 },
          { "date": "2025-10", "revenue": 1020000, "count": 17 },
          { "date": "2025-11", "revenue": 1080000, "count": 18 },
          { "date": "2025-12", "revenue": 1140000, "count": 19 },
          { "date": "2026-01", "revenue": 1260000, "count": 21 },
          { "date": "2026-02", "revenue": 1380000, "count": 23 }
        ]
      }
    ]
  }
}
```

### 필드 설명

**summary (핵심 지표)**

| 필드 | 타입 | 설명 |
|------|------|------|
| `totalServiceCount` | Integer | 기간 내 서비스 이용 총 건수 |
| `uniqueServiceTypes` | Integer | 이용된 서비스 종류 수 |
| `averagePrice` | Long | 서비스 평균 가격 (원) |
| `categoryCount` | Integer | 이용된 카테고리 수 |
| `mostPopularService` | String | 가장 많이 이용된 서비스명 (건수 기준) |
| `mostProfitableService` | String | 가장 높은 매출 서비스명 (매출 기준) |

**comparison (전기간 대비 변화율)** - `compareWith` 파라미터 전달 시에만 포함, 미전달 시 `null`

| 필드 | 타입 | 설명 |
|------|------|------|
| `totalServiceCountChange` | Double | 서비스 이용 건수 변화율 (%) |
| `averagePriceChange` | Double | 평균 가격 변화율 (%) |

**serviceRankings (서비스 랭킹)** - 매출 순 정렬

| 필드 | 타입 | 설명 |
|------|------|------|
| `rank` | Integer | 순위 (1부터 시작) |
| `serviceId` | Long | 서비스 ID |
| `serviceName` | String | 서비스명 |
| `categoryId` | Long | 카테고리 ID |
| `categoryName` | String | 카테고리명 |
| `reservationCount` | Integer | 예약 건수 |
| `totalRevenue` | Long | 총 매출 (원) |
| `averagePrice` | Long | 평균 가격 (원) |
| `revenuePercentage` | Double | 전체 매출 대비 비중 (%) |
| `averageDuration` | Integer | 평균 소요 시간 (분) |
| `completionRate` | Double | 완료율 (%) |

**categoryDistribution (카테고리별 분포)** - Pie/Donut 차트용

| 필드 | 타입 | 설명 |
|------|------|------|
| `categoryId` | Long | 카테고리 ID |
| `categoryName` | String | 카테고리명 |
| `serviceCount` | Integer | 해당 카테고리 서비스 종류 수 |
| `reservationCount` | Integer | 예약 건수 |
| `revenue` | Long | 총 매출 (원) |
| `percentage` | Double | 전체 매출 대비 비중 (%) |

**serviceTrend (서비스 매출 추이)** - TOP 5 서비스, 월별 집계

| 필드 | 타입 | 설명 |
|------|------|------|
| `serviceId` | Long | 서비스 ID |
| `serviceName` | String | 서비스명 |
| `trend` | Array | 월별 추이 데이터 |
| `trend[].date` | String | 년월 (`2026-02` 형식) |
| `trend[].revenue` | Long | 해당 월 매출 (원) |
| `trend[].count` | Integer | 해당 월 이용 건수 |

---

## 6. 공통 사항

### 인증

모든 통계 API는 Bearer Token 인증이 필요합니다.

```
Authorization: Bearer {accessToken}
```

### 권한

- **ADMIN**: 자신이 관리하는 매장의 통계 조회 가능
- **OWNER**: 자신이 소유한 매장의 통계 조회 가능
- **SUPER_ADMIN**: 모든 매장의 통계 조회 가능

모든 API에서 `userDetails.getUser().canAccessBusiness(businessId)` 권한 체크를 수행합니다.

### 날짜 형식

| 용도 | 형식 | 예시 |
|------|------|------|
| Query Parameter (startDate, endDate) | `yyyy-MM-dd` | `2026-02-01` |
| Response - 일별 | `yyyy-MM-dd` | `2026-02-01` |
| Response - 월별 | `yyyy-MM` | `2026-02` |

### 에러 코드 (ST001 ~ ST005)

| 코드 | HTTP | 메시지 |
|------|------|--------|
| ST001 | 400 | 시작일은 종료일보다 이전이어야 합니다 |
| ST002 | 400 | 조회 가능 기간은 최대 1년입니다 |
| ST003 | 400 | 유효하지 않은 집계 단위입니다 (daily/weekly/monthly) |
| ST004 | 404 | 통계 조회 대상 직원을 찾을 수 없습니다 |
| ST005 | 404 | 통계 조회 대상 카테고리를 찾을 수 없습니다 |

### 에러 응답 형식

```json
{
  "status": "error",
  "error": {
    "code": "ST001",
    "message": "시작일은 종료일보다 이전이어야 합니다"
  }
}
```

### 공통 비즈니스 에러

| 코드 | HTTP | 메시지 | 발생 상황 |
|------|------|--------|----------|
| B001 | 404 | 매장을 찾을 수 없습니다 | businessId 유효하지 않음 |
| B003 | 403 | 해당 매장에 접근 권한이 없습니다 | 권한 없는 매장 접근 |
| A001 | 401 | 인증이 필요합니다 | 토큰 미제공 |
| A002 | 401 | 유효하지 않은 토큰입니다 | 토큰 검증 실패 |

---

## 7. 기존 API와의 관계

### 기존 대시보드 API (변경 없음)

| API | 용도 | 변경 여부 |
|-----|------|----------|
| `GET /api/businesses/{businessId}/dashboard` | 기본 대시보드 (오늘 요약) | 변경 없음 |
| `GET /api/businesses/{businessId}/dashboard/stats` | 기간별 통계 (간단 요약) | 변경 없음 |
| `GET /api/businesses/{businessId}/dashboard/goals` | 목표 달성률 | 변경 없음 |

### 대시보드 vs 통계 API 차이점

| 항목 | 대시보드 (`/dashboard`) | 통계 (`/statistics`) |
|------|------------------------|---------------------|
| 목적 | 빠른 현황 파악 | 심층 분석 |
| 데이터 범위 | 오늘/주간/월간 요약 | 자유 기간 설정 |
| 비교 기능 | 없음 | 전기간/전년 동기 비교 |
| 차트 데이터 | 간단한 추이 | 히트맵, 레이더, 분포 등 |
| 집계 단위 | 고정 | daily/weekly/monthly 선택 |
| 세분화 | 없음 | 서비스별, 직원별, 카테고리별 |

---

## 8. API 엔드포인트 요약

| # | Method | URL | 설명 |
|---|--------|-----|------|
| 1 | `GET` | `/api/businesses/{businessId}/statistics/revenue` | 매출 분석 |
| 2 | `GET` | `/api/businesses/{businessId}/statistics/reservations` | 예약 분석 |
| 3 | `GET` | `/api/businesses/{businessId}/statistics/customers` | 고객 분석 |
| 4 | `GET` | `/api/businesses/{businessId}/statistics/staff` | 직원 성과 |
| 5 | `GET` | `/api/businesses/{businessId}/statistics/services` | 서비스 분석 |

---

## 9. 프론트엔드 구현 가이드

### API 호출 예시

**매출 분석 (2월 전체, 전기간 비교 포함)**
```
GET /api/businesses/1/statistics/revenue?startDate=2026-02-01&endDate=2026-02-28&groupBy=daily&compareWith=PREVIOUS_PERIOD
```

**예약 분석 (주별 집계)**
```
GET /api/businesses/1/statistics/reservations?startDate=2026-02-01&endDate=2026-02-28&groupBy=weekly
```

**고객 분석 (전년 동기 비교)**
```
GET /api/businesses/1/statistics/customers?startDate=2026-02-01&endDate=2026-02-28&compareWith=LAST_YEAR
```

**직원 성과 (특정 직원만)**
```
GET /api/businesses/1/statistics/staff?startDate=2026-02-01&endDate=2026-02-28&staffId=1
```

**서비스 분석 (특정 카테고리 필터)**
```
GET /api/businesses/1/statistics/services?startDate=2026-02-01&endDate=2026-02-28&categoryId=1
```

### compareWith 파라미터 사용 가이드

| 값 | 동작 | 예시 |
|----|------|------|
| `PREVIOUS_PERIOD` | 동일 길이의 직전 기간과 비교 | 2/1~2/28 요청 시, 1/4~1/31과 비교 |
| `LAST_YEAR` | 전년 동기와 비교 | 2026-02-01~02-28 요청 시, 2025-02-01~02-28과 비교 |
| 미전달 | 비교 데이터 없음 | `comparison` 필드가 `null` |

**PREVIOUS_PERIOD 계산 로직:**
- 비교 종료일 = startDate - 1일
- 비교 시작일 = 비교 종료일 - (endDate - startDate)일

### 차트 타입 추천

| 데이터 | 추천 차트 | 라이브러리 예시 |
|--------|----------|---------------|
| `revenueTrend` | Line Chart / Area Chart | recharts `<AreaChart>` |
| `revenueByService` | Horizontal Bar Chart | recharts `<BarChart layout="vertical">` |
| `revenueByPaymentMethod` | Pie Chart / Donut Chart | recharts `<PieChart>` |
| `goals` | Progress Bar / Gauge | 커스텀 Progress 컴포넌트 |
| `reservationTrend` | Stacked Area Chart | recharts `<AreaChart>` (stacked) |
| `hourlyHeatmap` | Heatmap | 커스텀 Grid 또는 nivo `<HeatMap>` |
| `statusDistribution` | Donut Chart | recharts `<PieChart innerRadius>` |
| `dailyDistribution` | Bar Chart | recharts `<BarChart>` |
| `customerTrend` | Stacked Bar Chart | recharts `<BarChart>` (stacked) |
| `segments` | Treemap / Pie Chart | recharts `<Treemap>` |
| `returningRateTrend` | Line Chart | recharts `<LineChart>` |
| `ltvDistribution` | Histogram (Bar) | recharts `<BarChart>` |
| `staffPerformances` | Table / Bar Chart | 테이블 + 정렬 기능 |
| `staffRevenueTrend` | Multi-Line Chart | recharts `<LineChart>` (다중 시리즈) |
| `staffRadar` | Radar Chart | recharts `<RadarChart>` |
| `serviceRankings` | Table / Horizontal Bar | 테이블 + 순위 표시 |
| `categoryDistribution` | Donut Chart | recharts `<PieChart>` |
| `serviceTrend` | Multi-Line Chart | recharts `<LineChart>` (다중 시리즈) |

### 히트맵 렌더링 주의사항

`hourlyHeatmap`의 `hours` 배열에는 예약이 존재하는 시간대만 포함됩니다. 히트맵을 렌더링할 때 전체 시간대(예: 9~21시)를 표시하려면, 빠져있는 시간대는 `count: 0`으로 채워야 합니다.

```typescript
// 예시: 빈 시간대 채우기
const BUSINESS_HOURS = Array.from({ length: 13 }, (_, i) => i + 9); // 9~21

function fillHours(hours: { hour: number; count: number }[]) {
  const hourMap = new Map(hours.map(h => [h.hour, h.count]));
  return BUSINESS_HOURS.map(hour => ({
    hour,
    count: hourMap.get(hour) ?? 0
  }));
}
```

### Radar 차트 축 레이블 매핑

```typescript
const RADAR_LABELS: Record<string, string> = {
  reservationVolume: "예약량",
  revenue: "매출",
  completionRate: "완료율",
  customerSatisfaction: "고객 만족도",
  efficiency: "효율성"
};
```

### 주의사항

1. **날짜 형식**: Query parameter는 반드시 `yyyy-MM-dd` 형식 (예: `2026-02-01`)
2. **금액 단위**: 모든 금액은 원(Won) 단위 정수 (소수점 없음)
3. **비율 단위**: 모든 퍼센트 값은 소수점 1자리까지 (예: `85.2`)
4. **Radar 값**: 0~100 정수 범위
5. **요일 기준**: ISO 8601 (1=월요일 ~ 7=일요일)
6. **comparison 필드**: `compareWith` 미전달 시 `null` 반환 -- `undefined`가 아닌 `null`
7. **goals 필드**: 매출 목표 미설정 시 `revenueGoal`, `revenueAchievementRate`는 `null`
8. **LTV max**: `ltvDistribution` 마지막 구간의 `max`는 `null` (상한 없음)
9. **기간 제한**: startDate~endDate 범위는 최대 365일

---

## 10. Swagger UI

모든 API는 Swagger UI에서 테스트 가능합니다:
```
http://localhost:8080/swagger-ui.html
```

**Tag:** `통계 분석` - 매출/예약/고객/직원/서비스 통계 분석 API
